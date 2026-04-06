package cn.reddragon.eportal.nt.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.reddragon.eportal.nt.data.AccountItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    accounts: List<AccountItem>,
    selectedAccountId: String?,
    onSelectAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onAddAccount: (AccountItem) -> Unit,
    onEditAccount: (String, AccountItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountItem?>(null) }
    var pendingDeleteStudentId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp, vertical = 16.dp)
    ) {
        if (accounts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.2F)
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "什么也没有 (´・ω・`)",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "点击右下角按钮添加账号",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(items = accounts, key = { it.studentId }) { account ->
                    AccountCard(
                        account = account,
                        selected = account.studentId == selectedAccountId,
                        onClick = { onSelectAccount(account.studentId) },
                        onLongClick = { editingAccount = account }
                    )
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showAddDialog = true },
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
            //.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                //modifier = Modifier.size(36.dp)
            )
        }
    }
    
    if (showAddDialog) {
        ManageAccountDialog(
            title = "添加账号",
            onConfirm = { username, studentId, password ->
                val normalizedStudentId = studentId.trim()
                val finalUsername = username.trim().ifBlank { normalizedStudentId }
                if (accounts.any { it.studentId == normalizedStudentId }) {
                    Toast.makeText(context, "账号已存在", Toast.LENGTH_SHORT).show()
                } else {
                    onAddAccount(
                        AccountItem(
                            username = finalUsername,
                            studentId = normalizedStudentId,
                            password = password
                        )
                    )
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }
    
    editingAccount?.let { account ->
        ManageAccountDialog(
            title = "编辑账号",
            initialUsername = account.username,
            initialStudentId = account.studentId,
            initialPassword = account.password,
            onConfirm = { username, studentId, password ->
                val normalizedStudentId = studentId.trim()
                val finalUsername = username.trim().ifBlank { normalizedStudentId }
                
                val duplicateExists = accounts.any {
                    it.studentId == normalizedStudentId && it.studentId != account.studentId
                }
                if (duplicateExists) {
                    Toast.makeText(context, "账号已存在", Toast.LENGTH_SHORT).show()
                    return@ManageAccountDialog
                }
                
                onEditAccount(
                    account.studentId,
                    AccountItem(
                        username = finalUsername,
                        studentId = normalizedStudentId,
                        password = password
                    )
                )
                editingAccount = null
            },
            onDelete = {
                pendingDeleteStudentId = account.studentId
                editingAccount = null
            },
            onDismiss = { editingAccount = null }
        )
    }
    
    pendingDeleteStudentId?.let { studentId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteStudentId = null },
            title = { Text("确认删除") },
            text = { Text("确定删除账号 $studentId 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAccount(studentId)
                        pendingDeleteStudentId = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteStudentId = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun AccountCard(
    account: AccountItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "account_card_color"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "account_card_scale"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(MaterialTheme.shapes.extraLarge)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 5.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                //.fillMaxWidth()
                ,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null)
                    Text(text = account.username)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Numbers, null)
                    Text(text = account.studentId)
                }
            }
            
            AnimatedContent(selected) {
                if (it) {
                    Icon(
                        modifier = Modifier
                            .scale(1.2F)
                            .padding(8.dp, 0.dp),
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        //tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ManageAccountDialog(
    title: String,
    initialUsername: String = "",
    initialStudentId: String = "",
    initialPassword: String = "",
    onConfirm: (String, String, String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var username by remember(initialUsername) { mutableStateOf(initialUsername) }
    var studentId by remember(initialStudentId) { mutableStateOf(initialStudentId) }
    val passwordState = rememberTextFieldState(initialText = initialPassword)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it.filter(Char::isDigit) },
                    label = { Text("学号") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { if (studentId.isBlank()) Text("必填") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    supportingText = { if (username.isBlank()) Text("选填，默认使为学号") },
                    singleLine = true
                )
                OutlinedSecureTextField(
                    state = passwordState,
                    label = { Text("密码") },
                    supportingText = { if (passwordState.text.isBlank()) Text("必填") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val password = passwordState.text.toString()
                    if (studentId.isNotBlank() && password.isNotBlank()) {
                        onConfirm(username, studentId, password)
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("删除")
                    }
                }
            }
        }
    )
}
