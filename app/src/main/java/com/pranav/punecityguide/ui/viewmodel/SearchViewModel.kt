package com.pranav.punecityguide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.repository.AttractionRepository
import com.pranav.punecityguide.data.repository.SyncAuditRepository
import com.pranav.punecityguide.domain.attraction.SearchAttractionsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<Attraction> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val searchAttractionsUseCase: SearchAttractionsUseCase,
    private val auditRepository: SyncAuditRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState
                .debounce(250)
                .map { it.query.trim() }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.length >= 2) searchAttractionsUseCase(query) else flowOf(emptyList())
                }
                .collect { results ->
                    if (results.isNotEmpty()) {
                        auditRepository.log("SEARCH_SUCCESS", "Query: '${_uiState.value.query}'", "Results: ${results.size}")
                    } else if (_uiState.value.query.length >= 2) {
                        auditRepository.log("SEARCH_EMPTY", "No results for: '${_uiState.value.query}'")
                    }
                    _uiState.value = _uiState.value.copy(
                        results = results,
                        isLoading = false
                    )
                }
        }
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            isLoading = query.trim().length >= 2
        )
    }

    companion object {
        fun factory(
            repository: AttractionRepository,
            auditRepository: SyncAuditRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val useCase = SearchAttractionsUseCase(repository)
                    return SearchViewModel(useCase, auditRepository) as T
                }
            }
        }
    }
}
