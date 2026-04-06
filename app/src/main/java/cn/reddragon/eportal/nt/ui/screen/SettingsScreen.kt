package cn.reddragon.eportal.nt.ui.screen

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        color = MaterialTheme.colorScheme.primaryFixed
    )
}

@Composable
fun Description(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}