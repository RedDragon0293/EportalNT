package cn.reddragon.eportal.nt.auth

import androidx.lifecycle.viewModelScope
import cn.reddragon.eportal.nt.data.AccountItem
import cn.reddragon.eportal.nt.ui.viewmodel.EPortalViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class UserStatusTracker(val viewModel: EPortalViewModel) {
    private var pollingJob: Job? = null
    
    private var pollingIntervalSeconds: Int = 10
    private var autoLoginWhenOffline: Boolean = true
    private var selectedAccountForAutoLogin: AccountItem? = null
    private var selectedServiceTypeForAutoLogin: ServiceType = ServiceType.WAN
    
    var autoLoginPausedByManualLogout: Boolean = false
    
    fun startPolling(
        intervalSeconds: Int
    ) {
        val shouldRestart = pollingIntervalSeconds != intervalSeconds
        pollingIntervalSeconds = intervalSeconds
        
        if (intervalSeconds == 0) {
            stopPolling()
            return
        }
        
        if (shouldRestart) {
            restartPolling()
            return
        }
        
        if (pollingJob?.isActive == true) {
            return
        }
        launchPollingJob()
    }
    
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    
    fun restartPolling() {
        if (pollingJob == null && pollingIntervalSeconds <= 0) {
            return
        }
        pollingJob?.cancel()
        launchPollingJob()
    }
    
    private fun launchPollingJob() {
        pollingJob = viewModel.viewModelScope.launch {
            while (isActive) {
                if (pollingIntervalSeconds == 0) {
                    stopPolling()
                    return@launch
                }
                delay((pollingIntervalSeconds * 1_000L).milliseconds)
                
                viewModel.updateUserStatus()
                
                if (autoLoginWhenOffline && !autoLoginPausedByManualLogout && !viewModel.uiState.value.isOnline) {
                    selectedAccountForAutoLogin?.let { account ->
                        viewModel.loginAndUpdate(account, selectedServiceTypeForAutoLogin)
                    }
                }
            }
        }
    }
    
    fun updatePollingParam(autoLogin: Boolean, account: AccountItem?, service: ServiceType) {
        autoLoginWhenOffline = autoLogin
        selectedAccountForAutoLogin = account
        selectedServiceTypeForAutoLogin = service
    }
}