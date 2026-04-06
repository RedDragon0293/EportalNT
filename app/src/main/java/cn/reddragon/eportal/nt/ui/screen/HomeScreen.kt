package cn.reddragon.eportal.nt.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.reddragon.eportal.nt.auth.ServiceType
import cn.reddragon.eportal.nt.data.UserStatus
import cn.reddragon.eportal.nt.ui.model.HomeScreenUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeScreenUiState,
    selectedAccountDisplay: String,
    selectedServiceType: ServiceType,
    onServiceTypeChange: (ServiceType) -> Unit,
    onStatusCardClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(),
            //.padding(vertical = 16.dp, horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OnlineStatusCard(
                isOnline = state.isOnline,
                selectedAccountDisplay = selectedAccountDisplay,
                isSyncing = state.isSyncing,
                onClick = onStatusCardClick
            )
            AnimatedContent(
                targetState = state.isOnline,
                label = "home_online_offline_content",
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220)) { it / 8 }) togetherWith
                            (fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(180)) { -it / 8 })
                }
            ) { online ->
                if (online) {
                    UserStatusCard(
                        state.onlineUser
                    )
                } else {
                    ServiceTypeSelector(
                        selectedServiceType = selectedServiceType,
                        onServiceTypeChange = onServiceTypeChange
                    )
                }
            }
            AnimatedContent(state.error && !state.isSyncing) { shouldBeShown ->
                if (shouldBeShown) {
                    ErrorCard(state.statusHint.orEmpty())
                }
            }
        }
        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            shape = CircleShape,
            onClick = onRefresh
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "刷新在线状态",
                //modifier = Modifier.size(36.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceTypeSelector(
    selectedServiceType: ServiceType,
    onServiceTypeChange: (ServiceType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
                .fillMaxWidth(),
            value = selectedServiceType.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("登录方式") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ServiceType.entries.forEach { serviceType ->
                DropdownMenuItem(
                    text = { Text(serviceType.displayName) },
                    onClick = {
                        onServiceTypeChange(serviceType)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun OnlineStatusCard(
    isOnline: Boolean,
    selectedAccountDisplay: String,
    isSyncing: Boolean,
    onClick: () -> Unit
) {
    val canOperate = selectedAccountDisplay != "未选择账号"
    val actionText = when {
        isSyncing -> "处理中..."
        !canOperate -> "请先在账号页选择账号"
        isOnline -> "点击登出"
        else -> "点击登录"
    }
    
    val containerColor by animateColorAsState(
        targetValue = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "status_card_container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isOnline) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "status_card_content"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isOnline) 1f else 0.92f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "status_icon_scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        //.clickable(onClick = onClick),
        //shape = RoundedCornerShape(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        enabled = !isSyncing,
        onClick = {
            if (canOperate && !isSyncing) onClick()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(Modifier.size(48.dp))
            } else {
                Icon(
                    imageVector = if (isOnline) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .scale(iconScale)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AnimatedContent(
                    targetState = if (isOnline) "在线" else "离线",
                    label = "status_text"
                ) { statusText ->
                    Text(
                        text = "状态：$statusText",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "当前选择账号：$selectedAccountDisplay",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.titleMedium
                )
                /*if (!statusHint.isNullOrBlank()) {
                    Text(
                        text = statusHint,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }*/
            }
        }
    }
}

@Composable
private fun UserStatusCard(
    status: UserStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "当前在线用户",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primaryFixed
            )
            InfoItem(
                icon = Icons.Default.AccountCircle,
                label = "姓名",
                value = status.username
            )
            
            InfoItem(
                icon = Icons.Default.Numbers,
                label = "学号",
                value = status.studentId
            )
            
            InfoItem(
                icon = Icons.Default.VpnKey,
                label = "登录方式",
                value = status.loginType.displayName
            )
            
            if (status.loginType == ServiceType.WAN) {
                InfoItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "余额",
                    value = status.fee.toString()
                )
                
                InfoItem(
                    icon = Icons.Default.AccessTime,
                    label = "剩余时长",
                    value = formatDurationSeconds(status.remainingDuration)
                )
                
                InfoItem(
                    icon = Icons.Default.Subscriptions,
                    label = "套餐",
                    value = status.pack
                )
            }
            InfoItem(
                icon = Icons.Default.Devices,
                label = "在线设备",
                value = status.devices.toString()
            )
            
            InfoItem(
                icon = Icons.Default.Dns,
                label = "IP地址",
                value = status.ip
            )
        }
    }
}

@Composable
private fun ErrorCard(
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
    }
}

@Suppress("unused")
@Composable
fun InfoItem(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatDurationSeconds(totalSeconds: Int): String {
    if (totalSeconds < 0) {
        return "∞"
    }
    
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    
    return buildString {
        if (h > 0) append("${h}小时")
        if (m > 0 || h > 0) append("${m}分")
        append("${s}秒")
    }
}