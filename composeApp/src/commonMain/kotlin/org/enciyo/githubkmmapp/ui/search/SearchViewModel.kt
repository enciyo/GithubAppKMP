package org.enciyo.githubkmmapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.enciyo.githubkmmapp.data.RemoteDataSource
import org.enciyo.githubkmmapp.data.model.SearchItemResponse


class SearchViewModel(
    private val remoteDataSource: RemoteDataSource
) : ViewModel() {

    private val _sideEffect = MutableSharedFlow<SideEffect>(extraBufferCapacity = 1)
    val sideEffect = _sideEffect.asSharedFlow()

    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    private val _search = MutableSharedFlow<String>(extraBufferCapacity = 1)

    init {
        registerSearch()
    }


    fun onInteraction(interact: Interactions) {
        when (interact) {
            is Interactions.OnBack -> _sideEffect.tryEmit(SideEffect.Back)
            is Interactions.OnClear -> _state.update { it.copy(keyword = "") }
            is Interactions.OnSearch -> onSearch(interact.keyword)
            is Interactions.OnResultClick -> {
                _sideEffect.tryEmit(SideEffect.NavigateToDetail(interact.result.url))
            }
        }
    }

    private fun onSearch(keyword: String) {
        _state.update { it.copy(keyword = keyword) }
        _search.tryEmit(keyword)
    }

    private fun registerSearch() {
        viewModelScope.launch {
            _search
                .debounce(550L)
                .collectLatest {
                    _state.updateAndGet { it.copy(isLoading = true) }
                    val user = remoteDataSource.searchUser(it)
                    val items = user.getOrNull()?.items.orEmpty()
                    val error = user.exceptionOrNull()?.message.orEmpty()
                    _state.update { it.copy(result = items, error = error, isLoading = false) }
                }
        }
    }


    sealed interface Interactions {
        data object OnBack : Interactions
        data class OnSearch(val keyword: String) : Interactions
        data object OnClear : Interactions
        data class OnResultClick(val result: SearchItemResponse) : Interactions
    }

    sealed interface SideEffect {
        data object Back : SideEffect
        data class NavigateToDetail(val result: String) : SideEffect
    }


    data class SearchState(
        val keyword: String = "",
        val result: List<SearchItemResponse> = emptyList(),
        val error: String = "",
        val isLoading: Boolean = false
    )


}