package cn.reddragon.eportal.nt.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("主页", Icons.Default.Home),
    ACCOUNT("账号", Icons.Default.ManageAccounts),
    SETTINGS("设置", Icons.Default.Settings)
}