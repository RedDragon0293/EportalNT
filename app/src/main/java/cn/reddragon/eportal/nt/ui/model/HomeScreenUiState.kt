package cn.reddragon.eportal.nt.ui.model

import cn.reddragon.eportal.nt.data.UserStatus

data class HomeScreenUiState(
    val isOnline: Boolean = false,
    val onlineUser: UserStatus = UserStatus.default(),
    val isSyncing: Boolean = false,
    val statusHint: String? = null,
    val error: Boolean = false,
)
