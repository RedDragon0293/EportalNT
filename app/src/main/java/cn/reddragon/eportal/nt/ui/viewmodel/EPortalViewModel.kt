package cn.reddragon.eportal.nt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.reddragon.eportal.nt.auth.ServiceType
import cn.reddragon.eportal.nt.auth.UserStatusTracker
import cn.reddragon.eportal.nt.data.AuthSyncSnapshot
import cn.reddragon.eportal.nt.data.CampusAuthRepository
import cn.reddragon.eportal.nt.data.AccountItem
import cn.reddragon.eportal.nt.ui.model.HomeScreenUiState
import cn.reddragon.eportal.nt.data.UserStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class EPortalViewModel(
    private val repository: CampusAuthRepository = CampusAuthRepository.INSTANCE
) : ViewModel() {
    val tracker = UserStatusTracker(this)
    
    private val _uiState = MutableStateFlow(
        HomeScreenUiState()
    )
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()
    
    /**
     * FloatActionButton 的回调函数
     */
    fun manualUpdate() {
        viewModelScope.launch {
            tracker.restartPolling()
            updateUserStatus()
        }
    }
    
    /**
     * 状态卡片的回调
     */
    fun login(account: AccountItem, serviceType: ServiceType) {
        viewModelScope.launch {
            
            // User explicitly asked to login, resume auto-login behavior.
            tracker.autoLoginPausedByManualLogout = false
            tracker.stopPolling()
            loginAndUpdate(account, serviceType)
            tracker.restartPolling()
        }
    }
    
    /**
     * 状态卡片的回调
     */
    fun logout() {
        viewModelScope.launch {
            
            // User explicitly logged out; keep auto-login paused until manual login.
            tracker.autoLoginPausedByManualLogout = true
            tracker.stopPolling()
            _uiState.update { it.copy(isSyncing = true) }
            val snapshot = repository.logout()
            applySnapshot(snapshot)
            tracker.restartPolling()
        }
    }
    
    suspend fun updateUserStatus() {
        /*_uiState.update { it.copy(isSyncing = true) }
        val snapshot = repository.updateUserStatus()
        applySnapshot(snapshot)*/
        
        var updated = false
        while (!updated) {
            _uiState.update { it.copy(isSyncing = true) }
            val ss = repository.updateUserStatus()
            if (ss.success != null) {
                applySnapshot(ss)
                updated = true
            }
            delay(1000.milliseconds)
        }
    }
    
    fun updatePollingParam(autoLogin: Boolean, account: AccountItem?, service: ServiceType) {
        tracker.updatePollingParam(autoLogin, account, service)
    }
    
    suspend fun loginAndUpdate(account: AccountItem, service: ServiceType) {
        _uiState.update { it.copy(isSyncing = true) }
        val snapshot = repository.login(account, service)
        applySnapshot(snapshot)
        
        if (!snapshot.isOnline) return
        
        updateUserStatus()
    }
    
    private fun applySnapshot(snapshot: AuthSyncSnapshot) {
        _uiState.value = _uiState.value.copy(
            isOnline = snapshot.isOnline,
            onlineUser = snapshot.userStatus ?: UserStatus.default(),
            isSyncing = false,
            statusHint = snapshot.statusHint,
            error = snapshot.success?.not() ?: false
        )
    }
    
    override fun onCleared() {
        tracker.stopPolling()
        super.onCleared()
    }
}

