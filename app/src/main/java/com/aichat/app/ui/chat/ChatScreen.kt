package com.aichat.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aichat.app.BuildConfig
import com.aichat.app.data.ChatMessage
import com.aichat.app.data.Conversation
import com.aichat.app.data.ModelConfig
import com.aichat.app.data.Role
import com.aichat.app.util.AppUpdateChecker
import com.aichat.app.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(uiState.messages.lastIndex)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Chat", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = uiState.currentModel?.name ?: "未选择模型",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showConversationSheet(true) }) {
                        Icon(Icons.Default.Menu, contentDescription = "对话列表")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.newConversation() }) {
                        Icon(Icons.Default.AddComment, contentDescription = "新对话")
                    }
                    IconButton(onClick = { viewModel.checkForUpdate(silent = false) }) {
                        Icon(Icons.Default.SystemUpdateAlt, contentDescription = "检查更新")
                    }
                    IconButton(onClick = { viewModel.showModelSheet(true) }) {
                        Icon(Icons.Default.Tune, contentDescription = "模型设置")
                    }
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "清空当前对话")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                text = uiState.inputText,
                isGenerating = uiState.isGenerating,
                enableThinking = uiState.currentModel?.enableThinking == true,
                enableWebSearch = uiState.currentModel?.enableWebSearch == true,
                onTextChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopGenerating,
                onToggleThinking = viewModel::toggleThinking,
                onToggleWebSearch = viewModel::toggleWebSearch
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.messages.isEmpty()) {
                EmptyState(currentModel = uiState.currentModel)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { msg ->
                        MessageBubble(msg)
                    }
                }
            }

            uiState.error?.let { err ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(err)
                }
            }

            uiState.updateMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearUpdateMessage() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        }
    }

    if (uiState.showConversationSheet) {
        ConversationSheet(
            conversations = uiState.conversations,
            currentId = uiState.currentConversationId,
            onSelect = viewModel::selectConversation,
            onDelete = viewModel::deleteConversation,
            onNew = viewModel::newConversation,
            onDismiss = { viewModel.showConversationSheet(false) }
        )
    }

    if (uiState.showModelSheet) {
        ModelSelectorSheet(
            models = uiState.models,
            currentId = uiState.currentModel?.id,
            onSelect = viewModel::selectModel,
            onEdit = { viewModel.showEditModel(it) },
            onAdd = {
                viewModel.showEditModel(
                    ModelConfig(
                        name = "",
                        baseUrl = "https://api.openai.com/v1",
                        apiKey = "",
                        model = "gpt-4o"
                    )
                )
            },
            onDismiss = { viewModel.showModelSheet(false) }
        )
    }

    if (uiState.showEditModel && uiState.editingModel != null) {
        EditModelDialog(
            config = uiState.editingModel!!,
            onSave = viewModel::saveModel,
            onDelete = {
                viewModel.deleteModel(it)
                viewModel.showEditModel(null)
            },
            onDismiss = { viewModel.showEditModel(null) }
        )
    }

    uiState.updateInfo?.let { info ->
        UpdateDialog(
            info = info,
            downloading = uiState.updateDownloading,
            progress = uiState.updateProgress,
            onDismiss = viewModel::dismissUpdate,
            onDownload = viewModel::downloadAndInstallUpdate,
            onInstall = viewModel::installDownloadedApk,
            onOpenPage = viewModel::openReleasePage
        )
    }

    if (uiState.updateChecking) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = { CircularProgressIndicator() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationSheet(
    conversations: List<Conversation>,
    currentId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "对话列表",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            TextButton(
                onClick = onNew,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("新建对话")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (conversations.isEmpty()) {
                Text(
                    "暂无对话",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                conversations.forEach { conv ->
                    ListItem(
                        headlineContent = {
                            Text(conv.title.ifBlank { "新对话" }, maxLines = 1)
                        },
                        supportingContent = {
                            Text(
                                "${conv.messages.size} 条消息",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            if (conv.id == currentId) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(Icons.Default.ChatBubbleOutline, null)
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { onDelete(conv.id) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(conv.id) }
                            .padding(horizontal = 8.dp),
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateDialog(
    info: AppUpdateChecker.ReleaseInfo,
    downloading: Boolean,
    progress: Float,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenPage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本 ${info.versionName}") },
        text = {
            Column {
                Text("当前版本：${BuildConfig.VERSION_NAME}")
                Spacer(modifier = Modifier.height(8.dp))
                if (info.body.isNotBlank()) {
                    Text(
                        info.body.take(400),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (downloading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            when {
                downloading -> TextButton(onClick = {}, enabled = false) { Text("下载中…") }
                progress >= 1f -> TextButton(onClick = onInstall) { Text("安装") }
                else -> TextButton(onClick = onDownload) { Text("下载更新") }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onOpenPage) { Text("网页") }
                TextButton(onClick = onDismiss) { Text("稍后") }
            }
        }
    )
}

@Composable
private fun EmptyState(currentModel: ModelConfig?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "开始对话吧",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (currentModel?.apiKey.isNullOrBlank())
                "请先点击右上角配置 API Key"
            else
                "当前模型：${currentModel?.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == Role.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = message.content.ifEmpty { if (message.isStreaming) "思考中..." else "" },
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontFamily = if (message.content.contains("```")) FontFamily.Monospace else FontFamily.Default
                )
                if (message.isStreaming && message.content.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBar(
    text: String,
    isGenerating: Boolean,
    enableThinking: Boolean,
    enableWebSearch: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onToggleThinking: () -> Unit,
    onToggleWebSearch: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = enableThinking,
                    onClick = onToggleThinking,
                    label = { Text("深度思考", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                    }
                )
                FilterChip(
                    selected = enableWebSearch,
                    onClick = onToggleWebSearch,
                    label = { Text("联网搜索", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(Icons.Default.Public, null, Modifier.size(16.dp))
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...") },
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (!isGenerating) onSend() }),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { if (isGenerating) onStop() else onSend() },
                    enabled = isGenerating || text.isNotBlank()
                ) {
                    Icon(
                        if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isGenerating) "停止" else "发送"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelectorSheet(
    models: List<ModelConfig>,
    currentId: String?,
    onSelect: (ModelConfig) -> Unit,
    onEdit: (ModelConfig) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "选择模型",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            models.forEach { model ->
                ListItem(
                    headlineContent = { Text(model.name) },
                    supportingContent = {
                        val flags = buildList {
                            if (model.enableThinking) add("思考")
                            if (model.enableWebSearch) add("联网")
                        }.joinToString(" · ")
                        Text(
                            listOfNotNull(model.model, flags.takeIf { it.isNotEmpty() })
                                .joinToString(" · "),
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        if (model.id == currentId) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.Default.RadioButtonUnchecked, null)
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { onEdit(model) }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(model) }
                        .padding(horizontal = 8.dp),
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            TextButton(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加新模型")
            }
        }
    }
}

@Composable
private fun EditModelDialog(
    config: ModelConfig,
    onSave: (ModelConfig) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(config.name) }
    var baseUrl by remember { mutableStateOf(config.baseUrl) }
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var model by remember { mutableStateOf(config.model) }
    var systemPrompt by remember { mutableStateOf(config.systemPrompt) }
    var temperature by remember { mutableStateOf(config.temperature.toString()) }
    var enableThinking by remember { mutableStateOf(config.enableThinking) }
    var enableWebSearch by remember { mutableStateOf(config.enableWebSearch) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (config.name.isBlank()) "添加模型" else "编辑模型") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("显示名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://api.deepseek.com") }
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model 名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("deepseek-v4-flash") }
                )
                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = { Text("Temperature") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("系统提示词") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("深度思考", modifier = Modifier.weight(1f))
                    Switch(checked = enableThinking, onCheckedChange = { enableThinking = it })
                }
                Text(
                    "适用于 DeepSeek 等支持 thinking 的接口",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("联网搜索", modifier = Modifier.weight(1f))
                    Switch(checked = enableWebSearch, onCheckedChange = { enableWebSearch = it })
                }
                Text(
                    "适用于通义等支持 enable_search 的接口",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        config.copy(
                            name = name.ifBlank { model },
                            baseUrl = baseUrl.trimEnd('/'),
                            apiKey = apiKey.trim(),
                            model = model.trim(),
                            temperature = temperature.toFloatOrNull() ?: 0.7f,
                            systemPrompt = systemPrompt,
                            enableThinking = enableThinking,
                            enableWebSearch = enableWebSearch
                        )
                    )
                },
                enabled = baseUrl.isNotBlank() && model.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                if (config.name.isNotBlank()) {
                    TextButton(onClick = { onDelete(config.id) }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}
