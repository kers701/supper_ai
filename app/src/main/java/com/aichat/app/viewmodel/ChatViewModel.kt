package com.aichat.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.app.data.ChatMessage
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
    val models: List<ModelConfig> = emptyList(),
    val currentModel: ModelConfig? = null,
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val error: String? = null,
    val showModelSheet: Boolean = false,
    val showEditModel: Boolean = false,
    val editingModel: ModelConfig? = null,
    val updateInfo: AppUpdateChecker.ReleaseInfo? = null,
    val updateChecking: Boolean = false,
    val updateDownloading: Boolean = false,
    val updateProgress: Float = 0f,
    val updateMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val modelRepo = ModelConfigRepository(application)
    private val chatRepo = ChatRepository()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private var downloadedApk: File? = null

    init {
        viewModelScope.launch {
            combine(modelRepo.models, modelRepo.currentModelId) { models, currentId ->
                models to currentId
            }.collect { (models, currentId) ->
                val current = models.find { it.id == currentId }
                    ?: models.find { it.isDefault }
                    ?: models.firstOrNull()
                _uiState.update {
                    it.copy(
                        models = models,
                        currentModel = current
                    )
                }
            }
        }
        checkForUpdate(silent = true)
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
                            val last = msgs.last()
                            msgs[msgs.lastIndex] = last.copy(content = fullContent, isStreaming = true)
                        }
                        s.copy(messages = msgs)
                    }
                }
                _uiState.update { s ->
                    val msgs = s.messages.toMutableList()
                    if (msgs.isNotEmpty()) {
                        val last = msgs.last()
                        msgs[msgs.lastIndex] = last.copy(isStreaming = false)
                    }
                    s.copy(messages = msgs, isGenerating = false)
                }
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
            }
        }
    }

    fun stopGenerating() {
        streamJob?.cancel()
        streamJob = null
        _uiState.update { s ->
            val msgs = s.messages.toMutableList()
            if (msgs.isNotEmpty()) {
                val last = msgs.last()
                if (last.isStreaming) {
                    msgs[msgs.lastIndex] = last.copy(isStreaming = false)
                }
            }
            s.copy(messages = msgs, isGenerating = false)
        }
    }

    fun clearChat() {
        stopGenerating()
        _uiState.update { it.copy(messages = emptyList(), error = null) }
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
                        it.copy(
                            updateInfo = result.info,
                            updateChecking = false,
                            updateMessage = null
                        )
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
                        _uiState.update {
                            it.copy(updateMessage = "请允许安装未知应用后，再点「安装」")
                        }
                    } else {
                        AppUpdateChecker.installApk(ctx, result.file)
                    }
                }
                is AppUpdateChecker.DownloadResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            updateDownloading = false,
                            updateMessage = "下载失败：${result.message}"
                        )
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
