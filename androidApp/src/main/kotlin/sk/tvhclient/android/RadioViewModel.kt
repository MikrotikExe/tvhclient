package sk.tvhclient.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.tvhclient.shared.Tvh
import sk.tvhclient.shared.api.ChannelRow

sealed class RadioState {
    data object Loading : RadioState()
    data object NoServer : RadioState()
    data class Loaded(val rows: List<ChannelRow>) : RadioState()
    data class Error(val message: String) : RadioState()
}

class RadioViewModel : ViewModel() {

    private val _state = MutableStateFlow<RadioState>(RadioState.Loading)
    val state: StateFlow<RadioState> = _state

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    fun setQuery(q: String) { _query.value = q }

    fun load() {
        val server = Tvh.store.active()
        if (server == null) {
            _state.value = RadioState.NoServer
            return
        }
        _state.value = RadioState.Loading
        viewModelScope.launch {
            try {
                val rows = withContext(Dispatchers.IO) {
                    val api = Tvh.apiFor(server)
                    try {
                        Tvh.channelRepository(server, api).radioRows()
                    } finally {
                        api.close()
                    }
                }
                _state.value = RadioState.Loaded(rows)
            } catch (e: Exception) {
                _state.value = RadioState.Error(e.message ?: "Chyba načítania")
            }
        }
    }
}
