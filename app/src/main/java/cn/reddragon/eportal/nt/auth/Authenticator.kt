package cn.reddragon.eportal.nt.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import cn.reddragon.eportal.nt.data.AccountItem
import cn.reddragon.eportal.nt.data.UserStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import java.net.ConnectException
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Dns

/**
 * Campus network authenticator.
 * Uses Ktor client to call ePortal APIs.
 */
object Authenticator {
    const val PORTAL_URL = "http://10.96.0.155/eportal"
    const val PORTAL_INT_URL = "$PORTAL_URL/InterFace.do?method="
    
    private val json = Json { ignoreUnknownKeys = true }
    private var appContext: Context? = null
    private var boundClient: HttpClient? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val clientMutex = Mutex()
    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val fallbackClient = HttpClient(OkHttp) {
        defaultRequest {
            header(
                HttpHeaders.UserAgent,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36 Edg/115.0.1901.203"
            )
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 3_000
            connectTimeoutMillis = 3_000
            socketTimeoutMillis = 3_000
        }
        engine {
            config {
                followRedirects(false)
            }
        }
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val cm = appContext?.getSystemService(ConnectivityManager::class.java) ?: return
        ensureNetworkCallbackRegistered(cm)
    }

    fun shutdown() {
        val cm = appContext?.getSystemService(ConnectivityManager::class.java)
        if (cm != null && networkCallback != null) {
            runCatching { cm.unregisterNetworkCallback(networkCallback!!) }
        }
        networkCallback = null
        authScope.launch { invalidateBoundClient() }
    }

    private fun ensureNetworkCallbackRegistered(cm: ConnectivityManager) {
        if (networkCallback != null) return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                authScope.launch {
                    clientMutex.withLock {
                        boundClient = createWifiBoundClient(network)
                    }
                }
            }

            override fun onLost(network: Network) {
                authScope.launch { invalidateBoundClient() }
            }

            /*override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                authScope.launch { invalidateBoundClient() }
            }*/
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        runCatching {
            cm.registerNetworkCallback(request, callback)
        }.onSuccess {
            networkCallback = callback
        }.onFailure {
            Log.w("Auth", "Register network callback failed", it)
        }
    }

    private suspend fun invalidateBoundClient() {
        clientMutex.withLock {
            boundClient?.close()
            boundClient = null
        }
    }

    private fun getClient(): HttpClient {
        //val context = appContext ?: return fallbackClient
        //val cm = context.getSystemService(ConnectivityManager::class.java) ?: return fallbackClient
        if (boundClient != null)
            return boundClient!!
        
        return fallbackClient
    }

    private fun createWifiBoundClient(network: Network): HttpClient {
        val okHttp = okhttp3.OkHttpClient.Builder()
            .socketFactory(network.socketFactory)
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    return network.getAllByName(hostname).toList()
                }
            })
            .followRedirects(false)
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build()

        return HttpClient(OkHttp) {
            engine {
                preconfigured = okHttp
            }
            defaultRequest {
                header(
                    HttpHeaders.UserAgent,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36 Edg/115.0.1901.203"
                )
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 3_000
                connectTimeoutMillis = 3_000
                socketTimeoutMillis = 3_000
            }
        }
    }
    
    suspend fun checkOnline(): AuthResult<Boolean> {
        val client = getClient()
        return try {
            val response = client.get("$PORTAL_URL/redirectortosuccess.jsp")
            //val location = response.headers[HttpHeaders.Location]
            //response.toString()
            val location = response.request.url.toString()
            val isOnline =
                if ("Http://123.123.123.123".equals(location, true))
                    false
                else location.contains("$PORTAL_URL/./success.jsp?")
            
            AuthResult.Success(isOnline)
        } catch (t: Throwable) {
            Log.e("Auth", "CheckOnline failed!", t)
            AuthResult.Failure(classifyThrowable(t, "在线状态检查失败"))
        }
    }
    
    suspend fun login(
        account: AccountItem,
        serviceType: ServiceType
    ): AuthResult<Boolean> {
        val client = getClient()
        if (account.studentId.isBlank() || account.password.isBlank()) {
            return AuthResult.Failure(AuthError(AuthErrorType.AUTH_FAILED, "账号或密码不能为空"))
        }
        
        val queryString = fetchQueryString()?: return AuthResult.Failure(AuthError(AuthErrorType.TIMEOUT, "无法获取QueryString"))
        
        val response = try {
            client.post("${PORTAL_INT_URL}login") {
                header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("userId", account.studentId)
                            append("password", account.password)
                            append("service", serviceType.authName)
                            append("queryString", queryString)
                            append("operatorPwd", "")
                            append("operatorUserId", "")
                            append("validcode", "")
                            append("passwordEncrypt", "false")
                        }
                    )
                )
            }
        } catch (t: Throwable) {
            Log.e("Auth", "Login failed!", t)
            return AuthResult.Failure(classifyThrowable(t, "登录请求失败"))
        }
        
        val result = response.bodyAsText()
        val resultJson = parseJsonObject(result)
        val resultText = resultJson?.get("result")?.jsonPrimitive?.contentOrNull
        val resultMessage = resultJson?.get("message")?.jsonPrimitive?.contentOrNull ?: ""
        
        return if (response.status.isSuccess() && resultText.equals("success", ignoreCase = true)) {
            AuthResult.Success(true)
        } else {
            AuthResult.Failure(AuthError(AuthErrorType.AUTH_FAILED, resultMessage))
        }
    }
    
    suspend fun fetchQueryString(): String? {
        val client = getClient()
        val response = try {
            client.get("http://123.123.123.123")
        } catch (t: Throwable) {
            Log.e("Auth", "Fetch query string failed!", t)
            return null
        }
        
        val result = response.bodyAsText().split('\'')
        return if (result.size != 3) "" else result[1]
    }
    
    suspend fun logout(userIndex: String): AuthResult<Boolean> {
        val client = getClient()
        val response = try {
            client.post("${PORTAL_INT_URL}logout") {
                header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                header(HttpHeaders.Referrer, "$PORTAL_URL/success.jsp?userIndex=${userIndex}")
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("userIndex", userIndex)
                        }
                    )
                )
            }
        } catch (t: Throwable) {
            Log.e("Auth", "Logout failed!", t)
            return AuthResult.Failure(classifyThrowable(t, "登出请求失败"))
        }
        
        val result = response.bodyAsText()
        val resultJson = parseJsonObject(result)
        val resultText = resultJson?.get("result")?.jsonPrimitive?.contentOrNull
        val resultMessage = resultJson?.get("message")?.jsonPrimitive?.contentOrNull ?: ""
        
        if (response.status.isSuccess() && resultText.equals("success", ignoreCase = true)) {
            return AuthResult.Success(true)
        }
        return AuthResult.Failure(AuthError(AuthErrorType.AUTH_FAILED, resultMessage))
    }
    
    suspend fun fetchOnlineUserInfo(userIndex: String): AuthResult<UserStatus> {
        val client = getClient()
        val response = try {
            client.post("${PORTAL_INT_URL}getOnlineUserInfo") {
                header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("userIndex", userIndex)
                        }
                    )
                )
            }
        } catch (t: Throwable) {
            Log.e("Auth", "Fetch online user info failed!", t)
            return AuthResult.Failure(classifyThrowable(t, "获取在线用户信息失败"))
        }
        
        val result = response.bodyAsText()
        val resultJson = parseJsonObject(result)
        val resultText = resultJson?.get("result")?.jsonPrimitive?.contentOrNull
        val resultMessage = resultJson?.get("message")?.jsonPrimitive?.contentOrNull ?: ""
        
        if (!response.status.isSuccess()) {
            return AuthResult.Failure(AuthError(AuthErrorType.AUTH_FAILED, "获取在线信息失败"))
        }
        
        try {
            if ("success" == resultText) {
                val username = resultJson["userName"]!!.jsonPrimitive.content
                val studentId = resultJson["userId"]!!.jsonPrimitive.content
                val ip = resultJson["userIp"]!!.jsonPrimitive.content
                val fee = resultJson["accountFee"]!!.jsonPrimitive.double
                val pack = resultJson["userPackage"]!!.jsonPrimitive.content
                
                var loginType = ServiceType.WAN
                
                //Log.i("ball", resultJson["ballInfo"]?.jsonArray ?: "null")
                val ballArray = json.parseToJsonElement(resultJson["ballInfo"]!!.jsonPrimitive.content).jsonArray
                
                val devices = ballArray[2].jsonObject["value"]!!.jsonPrimitive.int
                
                // 三大运营商
                if (ballArray[1].jsonObject["displayName"]!!.jsonPrimitive.content == "我的运营商") {
                    for (it in ServiceType.entries) {
                        if (it.authName.contains(ballArray[1].jsonObject["value"]!!.jsonPrimitive.content)) {
                            loginType = it
                            break
                        }
                    }
                    return AuthResult.Success(
                        UserStatus(
                            username = username,
                            studentId = studentId,
                            loginType = loginType,
                            pack = pack,
                            remainingDuration = -1,
                            devices = devices,
                            fee = fee,
                            ip = ip
                        )
                    )
                }
                // 校园网
                loginType = ServiceType.WAN
                return AuthResult.Success(
                    UserStatus(
                        username = username,
                        studentId = studentId,
                        loginType = loginType,
                        remainingDuration = ballArray[1].jsonObject["value"]!!.jsonPrimitive.int,
                        devices = devices,
                        fee = fee,
                        ip = ip,
                        pack = pack
                    )
                )
            } else if ("wait" != resultText) {
                return AuthResult.Failure(
                    AuthError(
                        AuthErrorType.UNKNOWN,
                        resultMessage
                    )
                )
            }
            return AuthResult.Wait()
        } catch (t: Throwable) {
            Log.e("Auth", "Process online user info failed!", t)
            return AuthResult.Failure(classifyThrowable(t, "解析在线用户信息失败"))
        }
    }
    
    suspend fun fetchUserIndex(): AuthResult<String> {
        val client = getClient()
        val response = runCatching {
            client.get("$PORTAL_URL/redirectortosuccess.jsp")
        }.getOrElse {
            return AuthResult.Failure(classifyThrowable(it, "无法获取用户会话标识"))
        }
        
        //val value = response.headers[HttpHeaders.Location]?.substringAfter("userIndex=", missingDelimiterValue = "")
        val value = response.request.url.toString().substringAfter("userIndex=", missingDelimiterValue = "")
        
        if (value.isNotBlank()) {
            return AuthResult.Success(value)
        }
        return AuthResult.Failure(AuthError(AuthErrorType.UNKNOWN, "无法从服务器响应中获取用户会话标识"))
    }
    
    private fun parseJsonObject(text: String): JsonObject? {
        return runCatching {
            json.parseToJsonElement(text).jsonObject
        }.getOrNull()
    }
    
    private fun classifyThrowable(throwable: Throwable, fallbackMessage: String): AuthError {
        return when (throwable) {
            is SocketTimeoutException,
            is HttpRequestTimeoutException -> AuthError(AuthErrorType.TIMEOUT, "请求超时")
            
            is UnknownHostException,
            is ConnectException,
            is SocketException,
            is UnresolvedAddressException -> AuthError(AuthErrorType.NETWORK, "网络连接不可用")
            
            else -> AuthError(AuthErrorType.UNKNOWN, throwable.message ?: fallbackMessage)
        }
    }
}

enum class AuthErrorType {
    TIMEOUT,
    NETWORK,
    AUTH_FAILED,
    UNKNOWN
}

data class AuthError(
    val type: AuthErrorType,
    val message: String
)

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Failure(val error: AuthError) : AuthResult<Nothing>()
    class Wait(): AuthResult<Nothing>()
}
