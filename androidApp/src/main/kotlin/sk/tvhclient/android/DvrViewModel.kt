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
    // channelOrder: meno kanala -> cislo (pre zoradenie archivu ako v zozname)
    data class Loaded(
        val entries: List<DvrEntry>,
        val channelOrder: Map<String, Int>
    ) : DvrState()
    data class Error(val message: String) : DvrState()
}

/**
 * DVR je read-only archiv (mazanie/planovanie su admin funkcie, tu nie su).
 * Nacita vsetky dokoncene nahravky + zoznam kanalov (pre poradie) raz;
 * navigaciu cez zlozky robi obrazovka v pamati.
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
                val (entries, order) = withContext(Dispatchers.IO) {
                    val api = Tvh.apiFor(server)
                    try {
                        val e = api.dvrFinished()
                        val order = api.channels()
                            .filter { it.number != null }
                            .associate { it.name to it.number!! }
                        e to order
                    } finally {
                        api.close()
                    }
                }
                _state.value = DvrState.Loaded(entries, order)
            } catch (e: Exception) {
                _state.value = DvrState.Error(e.message ?: "Chyba načítania")
            }
        }
    }
}
