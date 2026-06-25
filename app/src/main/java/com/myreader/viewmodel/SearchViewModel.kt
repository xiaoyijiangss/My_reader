package com.myreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myreader.data.repository.BookRepository
import com.myreader.data.source.BuiltinSources
import com.myreader.model.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val sources: List<com.myreader.data.source.SourceRule> = BuiltinSources.ALL
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository()
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun search() {
        val keyword = _uiState.value.query.trim()
        if (keyword.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null, results = emptyList())
            try {
                val results = repository.searchOnline(keyword)
                _uiState.value = _uiState.value.copy(
                    results = results,
                    isSearching = false,
                    error = if (results.isEmpty()) "未找到相关结果" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = "搜索失败: ${e.localizedMessage}"
                )
            }
        }
    }
}
