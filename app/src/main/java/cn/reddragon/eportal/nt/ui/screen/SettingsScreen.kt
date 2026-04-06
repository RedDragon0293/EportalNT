package cn.reddragon.eportal.nt.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

//@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    pollingIntervalSeconds: Int,
    autoLoginWhenOffline: Boolean,
    autoLoginStart: Boolean,
    onPollingIntervalChange: (Int) -> Unit,
    onAutoLoginWhenOfflineChange: (Boolean) -> Unit,
    onAutoLoginStartChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(pollingIntervalSeconds) {
        mutableFloatStateOf(pollingIntervalSeconds.toFloat())
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Title("在线状态同步间隔")
                Description("为防止外部操作（如浏览器）导致状态不同步，自动轮询在线状态")
                val sec = sliderValue.roundToInt()
                Text(
                    text = "当前：${if (sec > 0) "$sec 秒" else "禁用"}"
                )
                
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..60f,
                    onValueChangeFinished = {
                        onPollingIntervalChange(sliderValue.roundToInt())
                    },
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Title("启动时自动登录")
                Description("启动软件时时，离线自动登录")
                Switch(
                    checked = autoLoginStart,
                    onCheckedChange = onAutoLoginStartChange
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Title("离线状态下自动登录")
                Description("轮询在线状态时，离线自动登录")
                Switch(
                    checked = autoLoginWhenOffline,
                    onCheckedChange = onAutoLoginWhenOfflineChange
                )
            }
        }
    }
}

@Composable
fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun Description(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}