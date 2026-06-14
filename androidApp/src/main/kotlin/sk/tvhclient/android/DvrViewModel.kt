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
    // channelPicons: meno kanala -> picon URL (logo v archive)
    data class Loaded(
        val entries: List<DvrEntry>,
        val channelOrder: Map<String, Int>,
        val channelPicons: Map<String, String?>
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
                val result = withContext(Dispatchers.IO) {
                    val api = Tvh.apiFor(server)
                    try {
                        val e = api.dvrFinished()
                        val channels = api.channels()
                        val order = channels
                            .filter { it.number != null }
                            .associate { it.name to it.number!! }
                        val picons = channels.associate {
                            it.name to Tvh.piconUrl(server, it.iconPublicUrl)
                        }
                        Triple(e, order, picons)
                    } finally {
                        api.close()
                    }
                }
                _state.value = DvrState.Loaded(result.first, result.second, result.third)
            } catch (e: Exception) {
                _state.value = DvrState.Error(e.message ?: "Chyba načítania")
            }
        }
    }
}
