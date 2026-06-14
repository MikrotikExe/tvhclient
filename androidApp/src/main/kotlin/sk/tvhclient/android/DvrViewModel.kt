package sk.tvhclient.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.tvhclient.shared.Tvh
import sk.tvhclient.shared.model.DvrEntry

sealed class DvrState {
    data object Loading : DvrState()
    data object NoServer : DvrState()
    data class Loaded(val entries: List<DvrEntry>) : DvrState()
    data class Error(val message: String) : DvrState()
}

/**
 * DVR je read-only archiv (mazanie/planovanie su admin funkcie, tu nie su).
 * Nacita vsetky dokoncene nahravky raz; navigaciu cez zlozky (kanaly/datumy,
 * kategorie) robi obrazovka v pamati.
 */
class DvrViewModel : ViewModel() {

    private val _state = MutableStateFlow<DvrState>(DvrState.Loading)
    val state: StateFlow<DvrState> = _state

    fun load() {
        val server = Tvh.store.active()
        if (server == null) {
            _state.value = DvrState.NoServer
            return
        }
        _state.value = DvrState.Loading
        viewModelScope.launch {
            try {
                val entries = withContext(Dispatchers.IO) {
                    val api = Tvh.apiFor(server)
                    try { api.dvrFinished() } finally { api.close() }
                }
                _state.value = DvrState.Loaded(entries)
            } catch (e: Exception) {
                _state.value = DvrState.Error(e.message ?: "Chyba načítania")
            }
        }
    }
}
