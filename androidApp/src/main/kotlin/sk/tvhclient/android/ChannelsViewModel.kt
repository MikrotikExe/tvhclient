package sk.tvhclient.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.tvhclient.shared.Tvh
import sk.tvhclient.shared.api.ChannelCategory
import sk.tvhclient.shared.api.ChannelRow
import sk.tvhclient.shared.api.TvhApi

sealed class ChannelsState {
    data object Loading : ChannelsState()
    data class Loaded(
        val categories: List<ChannelCategory>,
        val allRows: List<ChannelRow>
    ) : ChannelsState()
    data class Error(val message: String) : ChannelsState()
    data object NoServer : ChannelsState()
}

class ChannelsViewModel : ViewModel() {

    private val _state = MutableStateFlow<ChannelsState>(ChannelsState.Loading)
    val state: StateFlow<ChannelsState> = _state

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // HTSP: kanal -> zoznam nadchadzajucich relacii (na auto-prechod na zozname)
    private val _epgMap = MutableStateFlow<Map<String, List<sk.tvhclient.shared.model.EpgEvent>>>(emptyMap())
    val epgMap: StateFlow<Map<String, List<sk.tvhclient.shared.model.EpgEvent>>> = _epgMap

    private var api: TvhApi? = null
    private var loadedOnce = false

    fun setQuery(q: String) { _query.value = q }

    /** Nacita len ak este nebolo nacitane (pri navrate na kartu neresetuje). */
    fun loadIfNeeded() {
        if (loadedOnce && _state.value is ChannelsState.Loaded) return
        load()
    }

    fun load(force: Boolean = false) {
        val server = Tvh.store.active()
        if (server == null) {
            _state.value = ChannelsState.NoServer
            return
        }
        _state.value = ChannelsState.Loading
        if (force && server.connectionMode == "htsp") {
            sk.tvhclient.shared.htsp.HtspData.clear(server.id)
        }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val a = Tvh.apiFor(server)
                    api?.close()
                    api = a
                    val repo = Tvh.channelRepository(server, a)
                    val cats = repo.load(force)
                    val all = repo.allRows(false)
                    cats to all
                }
                _state.value = ChannelsState.Loaded(result.first, result.second)
                loadedOnce = true

                // HTSP: now/next nie je v rychlom dumpe -> doplnime na pozadi
                if (server.connectionMode == "htsp") {
                    loadHtspNowNext(server)
                }
            } catch (e: Exception) {
                _state.value = ChannelsState.Error(e.message ?: "Chyba načítania")
            }
        }
    }

    private fun loadHtspNowNext(server: sk.tvhclient.shared.model.TvhServer) {
        viewModelScope.launch {
            val map = try {
                withContext(Dispatchers.IO) { Tvh.fetchEpgUpcoming(server) }
            } catch (e: Exception) { emptyMap() }
            if (map.isNotEmpty()) _epgMap.value = map
        }
    }

    override fun onCleared() {
        api?.close()
        super.onCleared()
    }
}
