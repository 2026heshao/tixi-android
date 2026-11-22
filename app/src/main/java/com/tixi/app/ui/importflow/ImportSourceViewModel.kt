package com.tixi.app.ui.importflow

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tixi.app.data.files.ImportFileStore
import com.tixi.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ImportSourceUiState(
    val configured: Boolean = false,
    val preparing: Boolean = false,
    val started: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class ImportSourceViewModel @Inject constructor(
    private val fileStore: ImportFileStore,
    private val runner: ImportRunner,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportSourceUiState())
    val state: StateFlow<ImportSourceUiState> = _state.asStateFlow()

    private var pendingCameraFile: File? = null

    init {
        viewModelScope.launch {
            settingsRepository.observeTokenConfigured().collect { configured ->
                _state.update { it.copy(configured = configured) }
            }
        }
    }

    /** 拍照前必须先建好目标文件，才能把 FileProvider Uri 交给相机 */
    fun createCameraTarget(): Uri? {
        return runCatching {
            val file = fileStore.newCameraFile()
            pendingCameraFile = file
            fileStore.uriFor(file)
        }.getOrNull()
    }

    fun onCameraResult(success: Boolean) {
        val file = pendingCameraFile
        pendingCameraFile = null
        if (!success || file == null || !file.exists() || file.length() == 0L) {
            _state.update { it.copy(message = "未获取到照片") }
            return
        }
        runner.start(file, sizeLabel(file.length()))
        _state.update { it.copy(started = true) }
    }

    fun pickDocument(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(preparing = true) }
            runCatching {
                val name = fileStore.displayName(uri)
                val file = fileStore.copyToPrivate(uri, name)
                runner.start(file, sizeLabel(file.length()))
            }.onSuccess {
                _state.update { it.copy(preparing = false, started = true) }
            }.onFailure {
                _state.update { it.copy(preparing = false, message = "读取文件失败，请换个文件试试") }
            }
        }
    }

    fun consumeStarted() = _state.update { it.copy(started = false) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun sizeLabel(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1048576.0)
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}
