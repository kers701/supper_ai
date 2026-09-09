package com.aichat.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.ChatMessage
import com.aichat.app.data.Conversation
import com.aichat.app.data.ConversationRepository
import com.aichat.app.data.ModelConfig
import com.aichat.app.data.ModelConfigRepository
import com.aichat.app.data.Role
import com.aichat.app.network.ChatRepository
import com.aichat.app.util.AppUpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: String? = null,
    val models: List<ModelConfig> = emptyList(),
    val currentModel: ModelConfig? = null,
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val error: String? = null,
    val showModelSheet: Boolean = false,
    val showEditModel: Boolean = false,
    val showConversationSheet: Boolean = false,
    val editingModel: ModelConfig? = null,
    val updateInfo: AppUpdateChecker.ReleaseInfo? = null,
    val updateChecking: Boolean = false,
    val updateDownloading: Boolean = false,
    val updateProgress: Float = 0f,
    val updateMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val modelRepo = ModelConfigRepository(application)
    private val conversationRepo = ConversationRepository(application)
    private val chatRepo = ChatRepository()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private var downloadedApk: File? = null
    private var persistJob: Job? = null

    init {
        viewModelScope.launch {
            combine(modelRepo.models, modelRepo.currentModelId) { models, currentId ->
                models to currentId
            }.collect { (models, currentId) ->
                val current = models.find { it.id == currentId }
                    ?: models.find { it.isDefault }
                    ?: models.firstOrNull()
                _uiState.update {
                    it.copy(models = models, currentModel = current)
                }
            }
        }
        viewModelScope.launch {
            combine(
                conversationRepo.conversations,
                conversationRepo.currentConversationId
            ) { list, currentId ->
                list to currentId
            }.collect { (list, currentId) ->
                val id = currentId?.takeIf { it.isNotBlank() }
                    ?: list.firstOrNull()?.id
                val current = list.find { it.id == id }
                _uiState.update { s ->
                    // 生成中不覆盖内存消息，避免流式被持久化回写冲掉
                    if (s.isGenerating && s.currentConversationId == id) {
                        s.copy(conversations = list, currentConversationId = id)
                    } else {
                        s.copy(
                            conversations = list,
                            currentConversationId = id,
                            messages = current?.messages ?: emptyList()
                        )
                    }
                }
            }
        }
        checkForUpdate(silent = true)
    }

    private fun persistCurrentConversation() {
        val state = _uiState.value
        val id = state.currentConversationId ?: return
        val title = state.messages
            .firstOrNull { it.role == Role.USER }
            ?.content
            ?.take(24)
            ?.ifBlank { "新对话" }
            ?: "新对话"
        val conv = Conversation(
            id = id,
            title = title,
            messages = state.messages.map { it.copy(isStreaming = false) },
            modelConfigId = state.currentModel?.id,
            updatedAt = System.currentTimeMillis()
        )
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            conversationRepo.upsert(conv)
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        val model = state.currentModel ?: return
        if (text.isEmpty() || state.isGenerating) return
        if (model.apiKey.isBlank()) {
            _uiState.update { it.copy(error = "请先配置该模型的 API Key") }
            return
        }

        // 无当前会话时自动新建
        if (state.currentConversationId.isNullOrBlank()) {
            val newId = java.util.UUID.randomUUID().toString()
            _uiState.update { it.copy(currentConversationId = newId) }
        }

        val userMsg = ChatMessage(role = Role.USER, content = text)
        val assistantMsg = ChatMessage(role = Role.ASSISTANT, content = "", isStreaming = true)

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg + assistantMsg,
                inputText = "",
                isGenerating = true,
                error = null
            )
        }

        streamJob = viewModelScope.launch {
            try {
                val history = _uiState.value.messages.dropLast(1)
                var fullContent = ""
                chatRepo.streamChat(model, history).collect { chunk ->
                    fullContent += chunk
                    _uiState.update { s ->
                        val msgs = s.messages.toMutableList()
                        if (msgs.isNotEmpty()) {
                            msgs[msgs.lastIndex] = msgs.last().copy(content = fullContent, isStreaming = true)
                        }
                        s.copy(messages = msgs)
                    }
                }
                _uiState.update { s ->
                    val msgs = s.messages.toMutableList()
                    if (msgs.isNotEmpty()) {
                        msgs[msgs.lastIndex] = msgs.last().copy(isStreaming = false)
                    }
                    s.copy(messages = msgs, isGenerating = false)
                }
                persistCurrentConversation()
            } catch (e: Exception) {
                _uiState.update { s ->
                    val msgs = s.messages.toMutableList()
                    if (msgs.isNotEmpty()) {
                        val last = msgs.last()
                        msgs[msgs.lastIndex] = last.copy(
                            content = last.content.ifEmpty { "[请求失败] ${e.message}" },
                            isStreaming = false
                        )
                    }
                    s.copy(messages = msgs, isGenerating = false, error = e.message)
                }
                persistCurrentConversation()
            }
        }
    }

    fun stopGenerating() {
        streamJob?.cancel()
        streamJob = null
        _uiState.update { s ->
            val msgs = s.messages.toMutableList()
            if (msgs.isNotEmpty() && msgs.last().isStreaming) {
                msgs[msgs.lastIndex] = msgs.last().copy(isStreaming = false)
            }
            s.copy(messages = msgs, isGenerating = false)
        }
        persistCurrentConversation()
    }

    fun newConversation() {
        stopGenerating()
        val id = java.util.UUID.randomUUID().toString()
        viewModelScope.launch {
            conversationRepo.upsert(
                Conversation(id = id, title = "新对话", messages = emptyList())
            )
            _uiState.update {
                it.copy(
                    currentConversationId = id,
                    messages = emptyList(),
                    error = null,
                    showConversationSheet = false
                )
            }
        }
    }

    fun selectConversation(id: String) {
        if (id == _uiState.value.currentConversationId) {
            _uiState.update { it.copy(showConversationSheet = false) }
            return
        }
        stopGenerating()
        viewModelScope.launch {
            conversationRepo.setCurrentId(id)
            val conv = _uiState.value.conversations.find { it.id == id }
            _uiState.update {
                it.copy(
                    currentConversationId = id,
                    messages = conv?.messages ?: emptyList(),
                    showConversationSheet = false,
                    error = null
                )
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationRepo.delete(id)
            if (_uiState.value.currentConversationId == id) {
                val remaining = _uiState.value.conversations.filter { it.id != id }
                if (remaining.isEmpty()) {
                    newConversation()
                } else {
                    selectConversation(remaining.first().id)
                }
            }
        }
    }

    fun clearChat() {
        stopGenerating()
        _uiState.update { it.copy(messages = emptyList(), error = null) }
        persistCurrentConversation()
    }

    fun showConversationSheet(show: Boolean) {
        _uiState.update { it.copy(showConversationSheet = show) }
    }

    fun toggleThinking() {
        val model = _uiState.value.currentModel ?: return
        viewModelScope.launch {
            modelRepo.addOrUpdateModel(model.copy(enableThinking = !model.enableThinking))
        }
    }

    fun toggleWebSearch() {
        val model = _uiState.value.currentModel ?: return
        viewModelScope.launch {
            modelRepo.addOrUpdateModel(model.copy(enableWebSearch = !model.enableWebSearch))
        }
    }

    fun selectModel(config: ModelConfig) {
        viewModelScope.launch {
            modelRepo.setCurrentModelId(config.id)
            _uiState.update { it.copy(showModelSheet = false) }
        }
    }

    fun showModelSheet(show: Boolean) {
        _uiState.update { it.copy(showModelSheet = show) }
    }

    fun showEditModel(config: ModelConfig?) {
        _uiState.update {
            it.copy(
                showEditModel = config != null,
                editingModel = config,
                showModelSheet = false
            )
        }
    }

    fun saveModel(config: ModelConfig) {
        viewModelScope.launch {
            modelRepo.addOrUpdateModel(config)
            _uiState.update { it.copy(showEditModel = false, editingModel = null) }
        }
    }

    fun deleteModel(id: String) {
        viewModelScope.launch {
            modelRepo.deleteModel(id)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun checkForUpdate(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(updateChecking = true, updateMessage = null) }
            }
            val result = withContext(Dispatchers.IO) { AppUpdateChecker.checkLatest() }
            when (result) {
                is AppUpdateChecker.CheckResult.UpdateAvailable -> {
                    _uiState.update {
                        it.copy(updateInfo = result.info, updateChecking = false, updateMessage = null)
                    }
                }
                is AppUpdateChecker.CheckResult.UpToDate -> {
                    _uiState.update {
                        it.copy(
                            updateChecking = false,
                            updateMessage = if (silent) null else "已是最新版本 ${result.current}"
                        )
                    }
                }
                is AppUpdateChecker.CheckResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            updateChecking = false,
                            updateMessage = if (silent) null else "检查更新失败：${result.message}"
                        )
                    }
                }
            }
        }
    }

    fun dismissUpdate() {
        _uiState.update { it.copy(updateInfo = null, updateProgress = 0f, updateDownloading = false) }
    }

    fun downloadAndInstallUpdate() {
        val info = _uiState.value.updateInfo ?: return
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update { it.copy(updateDownloading = true, updateProgress = 0f, updateMessage = null) }
            val result = withContext(Dispatchers.IO) {
                AppUpdateChecker.downloadApk(ctx, info) { p ->
                    _uiState.update { it.copy(updateProgress = p) }
                }
            }
            when (result) {
                is AppUpdateChecker.DownloadResult.Ok -> {
                    downloadedApk = result.file
                    _uiState.update { it.copy(updateDownloading = false, updateProgress = 1f) }
                    if (!AppUpdateChecker.canInstallPackages(ctx)) {
                        AppUpdateChecker.openInstallPermissionSettings(ctx)
                        _uiState.update { it.copy(updateMessage = "请允许安装未知应用后，再点「安装」") }
                    } else {
                        AppUpdateChecker.installApk(ctx, result.file)
                    }
                }
                is AppUpdateChecker.DownloadResult.Failed -> {
                    _uiState.update {
                        it.copy(updateDownloading = false, updateMessage = "下载失败：${result.message}")
                    }
                }
            }
        }
    }

    fun installDownloadedApk() {
        val file = downloadedApk ?: return
        val ctx = getApplication<Application>()
        if (!AppUpdateChecker.canInstallPackages(ctx)) {
            AppUpdateChecker.openInstallPermissionSettings(ctx)
            return
        }
        AppUpdateChecker.installApk(ctx, file)
    }

    fun openReleasePage() {
        val url = _uiState.value.updateInfo?.htmlUrl ?: return
        AppUpdateChecker.openReleasePage(getApplication(), url)
    }

    fun clearUpdateMessage() {
        _uiState.update { it.copy(updateMessage = null) }
    }
}
