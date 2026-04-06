package cn.reddragon.eportal.nt.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import cn.reddragon.eportal.nt.auth.Authenticator
import cn.reddragon.eportal.nt.auth.ServiceType
import cn.reddragon.eportal.nt.data.AccountPreferencesStore
import cn.reddragon.eportal.nt.data.CampusAuthRepository
import cn.reddragon.eportal.nt.data.StoredAccountState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CampusAuthTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val actionMutex = Mutex()
    private val repository = CampusAuthRepository.INSTANCE

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            actionMutex.withLock {
                Authenticator.initialize(applicationContext)
                refreshTileState()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            actionMutex.withLock {
                Authenticator.initialize(applicationContext)
                setTileState(
                    state = Tile.STATE_UNAVAILABLE,
                    subtitle = "处理中..."
                )

                val storedState = AccountPreferencesStore(applicationContext).accountStateFlow.first()
                val selectedAccount = pickSelectedAccount(storedState)
                if (selectedAccount == null) {
                    setTileState(
                        state = Tile.STATE_UNAVAILABLE,
                        subtitle = "未选择账号"
                    )
                    return@withLock
                }

                val selectedServiceType = ServiceType.entries.firstOrNull {
                    it.name == storedState.selectedServiceTypeName
                } ?: ServiceType.WAN

                val current = repository.updateUserStatus()
                if (current.isOnline) {
                    val op = repository.logout()
                    setTileStateBySnapshot(op.isOnline, op.statusHint)
                } else {
                    val op = repository.login(selectedAccount, selectedServiceType)
                    setTileStateBySnapshot(op.isOnline, op.statusHint)
                }
            }
        }
    }
    
    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun refreshTileState() {
        val storedState = AccountPreferencesStore(applicationContext).accountStateFlow.first()
        val selectedAccount = pickSelectedAccount(storedState)
        if (selectedAccount == null) {
            setTileState(
                state = Tile.STATE_UNAVAILABLE,
                subtitle = "未选择账号"
            )
            return
        }

        val snapshot = repository.updateUserStatus()
        setTileStateBySnapshot(snapshot.isOnline, snapshot.statusHint)
    }

    private fun pickSelectedAccount(storedState: StoredAccountState) =
        storedState.accounts.firstOrNull { it.studentId == storedState.selectedAccountId }
            ?: storedState.accounts.firstOrNull()

    private fun setTileStateBySnapshot(isOnline: Boolean, statusHint: String?) {
        if (isOnline) {
            setTileState(
                state = Tile.STATE_ACTIVE,
                subtitle = "已在线"
            )
        } else {
            setTileState(
                state = Tile.STATE_INACTIVE,
                subtitle = statusHint?.takeIf { it.isNotBlank() } ?: "未在线"
            )
        }
    }

    private fun setTileState(state: Int, subtitle: String) {
        qsTile?.apply {
            label = "校园网"
            this.state = state
            this.subtitle = subtitle
            updateTile()
        }
    }
}

