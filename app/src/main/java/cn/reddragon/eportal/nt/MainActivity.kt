package cn.reddragon.eportal.nt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.reddragon.eportal.nt.auth.Authenticator
import cn.reddragon.eportal.nt.auth.ServiceType
import cn.reddragon.eportal.nt.data.AccountPreferencesStore
import cn.reddragon.eportal.nt.data.StoredAccountState
import cn.reddragon.eportal.nt.ui.screen.AppTab
import cn.reddragon.eportal.nt.ui.screen.AccountScreen
import cn.reddragon.eportal.nt.ui.screen.HomeScreen
import cn.reddragon.eportal.nt.ui.screen.SettingsScreen
import cn.reddragon.eportal.nt.ui.theme.EportalNTTheme
import cn.reddragon.eportal.nt.ui.viewmodel.EPortalViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Authenticator.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            EportalNTTheme {
                CampusApp()
            }
        }
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            // Release network callback and bound client only on final destroy.
            Authenticator.shutdown()
        }
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampusApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val accountStore = remember(context) { AccountPreferencesStore(context) }
    val ePortalViewModel: EPortalViewModel = viewModel()

    val storedState by accountStore.accountStateFlow.collectAsState(
        initial = StoredAccountState(
            accounts = emptyList(),
            selectedAccountId = null,
            selectedServiceTypeName = ServiceType.entries.first().name,
            pollingIntervalSeconds = 10,
            autoLoginWhenOffline = true,
            autoLoginStart = true,
        )
    )
    val uiState by ePortalViewModel.uiState.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    val selectedServiceType = ServiceType.entries.firstOrNull {
        it.name == storedState.selectedServiceTypeName
    } ?: ServiceType.WAN

    val accounts = storedState.accounts
    val selectedAccountId = storedState.selectedAccountId?.takeIf { targetId ->
        accounts.any { account -> account.studentId == targetId }
    } ?: accounts.firstOrNull()?.studentId

    val currentTab = AppTab.valueOf(selectedTab)
    val selectedAccount = accounts.firstOrNull { account -> account.studentId == selectedAccountId }

    // 启动时执行
    LaunchedEffect(Unit) {
        ePortalViewModel.updateUserStatus()
        if (!ePortalViewModel.uiState.value.isOnline && storedState.autoLoginStart) {
            val account = storedState.accounts.firstOrNull { it.studentId == storedState.selectedAccountId }
            if (account != null) {
                ePortalViewModel.loginAndUpdate(account, selectedServiceType)
            }
        }
    }

    // 轮询交由 ViewModel 托管，Compose 仅同步配置。
    LaunchedEffect(storedState.pollingIntervalSeconds) {
        ePortalViewModel.tracker.startPolling(
            intervalSeconds = storedState.pollingIntervalSeconds
        )
    }
    // 更新参数
    LaunchedEffect(
        storedState.autoLoginWhenOffline,
        selectedAccount?.studentId,
        selectedServiceType.name
    ) {
        ePortalViewModel.updatePollingParam(
            autoLogin = storedState.autoLoginWhenOffline,
            account = selectedAccount,
            service = selectedServiceType
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentTab.label,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1f,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        label = "bottom_nav_${tab.name}"
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab.name },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.scale(scale)
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            label = "tab_content_transition",
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { it / 10 }) togetherWith
                        (fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(180)) { -it / 10 })
            },
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                AppTab.HOME -> {
                    HomeScreen(
                        state = uiState,
                        selectedAccountDisplay = selectedAccount?.username ?: "未选择账号",
                        selectedServiceType = selectedServiceType,
                        onServiceTypeChange = { serviceType ->
                            coroutineScope.launch {
                                accountStore.saveSelectedServiceTypeName(serviceType.name)
                            }
                        },
                        onStatusCardClick = {
                            if (uiState.isOnline) {
                                ePortalViewModel.logout()
                            } else if (selectedAccount != null) {
                                ePortalViewModel.login(
                                    selectedAccount,
                                    selectedServiceType,
                                )
                            }

                        },
                        onRefresh = {
                            ePortalViewModel.manualUpdate()
                        },
                        modifier = Modifier
                    )
                }

                AppTab.ACCOUNT -> {
                    AccountScreen(
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        onSelectAccount = { accountId ->
                            coroutineScope.launch {
                                accountStore.saveAccountState(accounts = accounts, selectedAccountId = accountId)
                            }
                        },
                        onDeleteAccount = { accountId ->
                            val updatedAccounts = accounts.filterNot { account -> account.studentId == accountId }
                            val updatedSelectedId = if (selectedAccountId == accountId) {
                                updatedAccounts.firstOrNull()?.studentId
                            } else {
                                selectedAccountId
                            }
                            coroutineScope.launch {
                                accountStore.saveAccountState(
                                    accounts = updatedAccounts,
                                    selectedAccountId = updatedSelectedId
                                )
                            }
                        },
                        onAddAccount = { newAccount ->
                            val updatedAccounts = accounts
                                .filterNot { account -> account.studentId == newAccount.studentId } + newAccount
                            val updatedSelectedId = selectedAccountId ?: newAccount.studentId
                            coroutineScope.launch {
                                accountStore.saveAccountState(
                                    accounts = updatedAccounts,
                                    selectedAccountId = updatedSelectedId
                                )
                            }
                        },
                        onEditAccount = { oldStudentId, editedAccount ->
                            val updatedAccounts = accounts
                                .filterNot { account -> account.studentId == oldStudentId }
                                .filterNot { account -> account.studentId == editedAccount.studentId } + editedAccount

                            val updatedSelectedId = if (
                                selectedAccountId == oldStudentId ||
                                selectedAccountId == editedAccount.studentId ||
                                selectedAccountId == null
                            ) {
                                editedAccount.studentId
                            } else {
                                selectedAccountId
                            }

                            coroutineScope.launch {
                                accountStore.saveAccountState(
                                    accounts = updatedAccounts,
                                    selectedAccountId = updatedSelectedId
                                )
                            }
                        },
                        modifier = Modifier
                    )
                }

                AppTab.SETTINGS -> {
                    SettingsScreen(
                        pollingIntervalSeconds = storedState.pollingIntervalSeconds,
                        autoLoginWhenOffline = storedState.autoLoginWhenOffline,
                        autoLoginStart = storedState.autoLoginStart,
                        onPollingIntervalChange = { seconds ->
                            coroutineScope.launch {
                                accountStore.savePollingIntervalSeconds(seconds)
                            }
                        },
                        onAutoLoginWhenOfflineChange = { enabled ->
                            coroutineScope.launch {
                                accountStore.saveAutoLoginWhenOffline(enabled)
                            }
                        },
                        onAutoLoginStartChange = { enabled ->
                            coroutineScope.launch {
                                accountStore.saveAutoLoginStart(enabled)
                            }
                        },
                        modifier = Modifier
                    )
                }
            }
        }
    }
}
