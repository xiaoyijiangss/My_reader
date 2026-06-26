package com.myreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.data.source.LegadoSource
import com.myreader.data.source.SourceHubClient
import com.myreader.data.source.SourceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SourceMgrUiState(
    val sources: List<LegadoSource> = emptyList(),
    val isImporting: Boolean = false,
    val importResult: String = "",
    val isFetching: Boolean = false,
    val fetchProgress: String = "",
    val error: String? = null
)

class SourceManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SourceMgrUiState())
    val uiState: StateFlow<SourceMgrUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            SourceManager.init(application)
            SourceManager.sources.collect { sources ->
                _uiState.value = _uiState.value.copy(sources = sources)
            }
        }
    }

    fun toggleSource(source: LegadoSource) {
        viewModelScope.launch {
            SourceManager.toggleSource(source)
        }
    }

    fun removeSource(source: LegadoSource) {
        viewModelScope.launch {
            SourceManager.removeSource(source)
            _uiState.value = _uiState.value.copy(importResult = "已删除: ${source.bookSourceName}")
        }
    }

    fun importFromJson(json: String) {
        if (json.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入书源JSON内容")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, importResult = "", error = null)
            try {
                val sources = SourceHubClient.importFromString(json)
                if (sources.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = "未识别到有效的书源格式，请检查JSON内容"
                    )
                } else {
                    SourceManager.addSources(sources)
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importResult = "成功导入 ${sources.size} 个书源"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = "导入失败: ${e.localizedMessage}"
                )
            }
        }
    }

    fun importFromUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetching = true, fetchProgress = "正在下载...", error = null)
            try {
                val sources = SourceHubClient.fetchFromUrl(url)
                if (sources.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isFetching = false,
                        error = "未获取到书源，请检查URL是否正确"
                    )
                } else {
                    SourceManager.addSources(sources)
                    _uiState.value = _uiState.value.copy(
                        isFetching = false,
                        fetchProgress = "成功",
                        importResult = "从URL导入了 ${sources.size} 个书源"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFetching = false,
                    error = "下载失败: ${e.localizedMessage}"
                )
            }
        }
    }

    fun fetchFromHub(hub: SourceHubClient.SourceHub) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isFetching = true,
                fetchProgress = "正在获取: ${hub.name}...",
                error = null
            )
            try {
                val sources = SourceHubClient.fetchFromHub(hub)
                if (sources.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isFetching = false,
                        error = "${hub.name} 未返回有效书源"
                    )
                } else {
                    SourceManager.addSources(sources)
                    _uiState.value = _uiState.value.copy(
                        isFetching = false,
                        fetchProgress = "完成",
                        importResult = "从 ${hub.name} 导入了 ${sources.size} 个书源"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFetching = false,
                    error = "获取失败: ${e.localizedMessage}"
                )
            }
        }
    }

    fun fetchAllHubs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isFetching = true,
                fetchProgress = "正在从所有仓库获取...",
                error = null
            )
            try {
                val sources = SourceHubClient.fetchAllHubs()
                if (sources.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isFetching = false,
                        error = "所有仓库均未返回有效书源，请检查网络连接"
                    )
                } else {
                    SourceManager.addSources(sources)
                    _uiState.value = _uiState.value.copy(
                        isFetching = false,
                        fetchProgress = "完成",
                        importResult = "共获取 ${sources.size} 个书源（含所有仓库）"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFetching = false,
                    error = "获取失败: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            SourceManager.clearAll()
            _uiState.value = _uiState.value.copy(importResult = "已清空所有书源")
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            SourceManager.resetToDefaults()
            _uiState.value = _uiState.value.copy(importResult = "已重置书源")
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
