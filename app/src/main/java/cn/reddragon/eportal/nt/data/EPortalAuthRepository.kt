package cn.reddragon.eportal.nt.data

import cn.reddragon.eportal.nt.auth.AuthError
import cn.reddragon.eportal.nt.auth.AuthErrorType
import cn.reddragon.eportal.nt.auth.AuthResult
import cn.reddragon.eportal.nt.auth.Authenticator
import cn.reddragon.eportal.nt.auth.ServiceType

class CampusAuthRepository(
    private val authenticator: Authenticator
) {
    private var online = false
    private var userIndex: String? = null
    private var userStatus: UserStatus? = null
    
    companion object {
        val INSTANCE = CampusAuthRepository()
    }
    
    private constructor(): this(Authenticator)
    
    suspend fun login(account: AccountItem, serviceType: ServiceType): AuthSyncSnapshot {
        if (online) {
            return AuthSyncSnapshot(true, userStatus, "当前已在线，请勿重复登录", false)
        }
        
        if (account.studentId.isBlank() || account.password.isBlank()) {
            return AuthSyncSnapshot(online, null, "账号或密码不能为空", false)
        }
        
        when (val result = authenticator.login(account, serviceType)) {
            is AuthResult.Success -> {
                online = true
                return AuthSyncSnapshot(true, null, "登录成功", true)
            }
            
            is AuthResult.Failure -> {
                return AuthSyncSnapshot(online, null, mapErrorMessage(result.error), false)
            }
            else -> return AuthSyncSnapshot(online, userStatus, "内部错误", false)
        }
    }
    
    suspend fun logout(): AuthSyncSnapshot {
        if (!online) {
            return AuthSyncSnapshot(false, null, "当前未在线", false)
        }
        
        checkUserIndex()?.let {
            return it
        }
        
        when (val result = authenticator.logout(userIndex!!)) {
            is AuthResult.Success -> {
                online = false
                userIndex = null
                userStatus = null
                return AuthSyncSnapshot(false, null, "登出成功", true)
            }
            is AuthResult.Failure -> return AuthSyncSnapshot(online, userStatus, mapErrorMessage(result.error), false)
            else -> return AuthSyncSnapshot(online, userStatus, "内部错误", false)
        }
    }
    
    suspend fun updateUserStatus(): AuthSyncSnapshot {
        var newOnline: Boolean
        when (val result = authenticator.checkOnline()) {
            is AuthResult.Success -> {
                newOnline = result.data
                //return AuthSyncSnapshot(online, userStatus, "在线状态更新成功")
            }
            is AuthResult.Failure -> {
                return AuthSyncSnapshot(online, userStatus, mapErrorMessage(result.error), false)
            }
            else -> return AuthSyncSnapshot(online, userStatus, "内部错误", false)
        }
        
        if (!newOnline) {
            if (online) { // 从在线变离线
                userIndex = null
                userStatus = null
                online = false
                return AuthSyncSnapshot(false, null, "在线状态发生变化", true)
            } else { // 一直离线
                return AuthSyncSnapshot(false, null, "当前未在线", true)
            }
        }
        
        online = true
        checkUserIndex()?.let {
            return it
        }
        
        when (val result = authenticator.fetchOnlineUserInfo(userIndex!!)) {
            is AuthResult.Success -> {
                userStatus = result.data
                return AuthSyncSnapshot(online, userStatus, "在线用户信息更新成功", true)
            }
            is AuthResult.Failure -> {
                return AuthSyncSnapshot(online, userStatus, mapErrorMessage(result.error), true)
            }
            is AuthResult.Wait -> {
                return AuthSyncSnapshot(online, userStatus, "服务器要求等待", null)
            }
        }
    }
    
    private suspend fun checkUserIndex(): AuthSyncSnapshot? {
        if (userIndex.isNullOrBlank()) {
            when (val result = authenticator.fetchUserIndex()) {
                is AuthResult.Success -> {
                    userIndex = result.data
                    return null
                }
                is AuthResult.Failure -> {
                    return AuthSyncSnapshot(online, userStatus, mapErrorMessage(result.error), false)
                }
                else -> return AuthSyncSnapshot(online, userStatus, "内部错误", false)
            }
        }
        return null
    }

    private fun mapErrorMessage(error: AuthError): String {
        return when (error.type) {
            AuthErrorType.TIMEOUT -> "请求超时：${error.message}"
            AuthErrorType.NETWORK -> "网络异常：${error.message}"
            AuthErrorType.AUTH_FAILED -> "认证错误：${error.message}"
            AuthErrorType.UNKNOWN -> "未知错误：${error.message}"
        }
    }
}

/**
 * @param userStatus 当不在线或刚刚登录尚未获取信息时，值应该为 null
 */
data class AuthSyncSnapshot(
    val isOnline: Boolean,
    val userStatus: UserStatus?,
    val statusHint: String,
    val success: Boolean?
)

