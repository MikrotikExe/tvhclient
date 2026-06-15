package sk.tvhclient.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.tvhclient.shared.Tvh
import sk.tvhclient.shared.model.EpgEvent

/**
 * EPG pre mriezku: nacita sa RAZ hromadne (vsetky kanaly na jednom spojeni) a
 * drzi v pamati, takze skrolovanie v TV programe je plynule a neprepina sa
 * stahovanie po kanaloch. Prezije prepnutie kariet (Activity-scoped).
 */
class EpgGridViewModel : ViewModel() {

    private val _epg = MutableStateFlow<Map<String, List<EpgEvent>>>(emptyMap())
    val epg: StateFlow<Map<String, List<EpgEvent>>> = _epg

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private var loadedOnce = false

    fun loadIfNeeded() {
        if (loadedOnce) return
        load(force = false)
    }

    fun refresh() = load(force = true)

    private fun load(force: Boolean) {
        val server = Tvh.store.active() ?: return
        if (_loading.value) return
        _loading.value = true
        viewModelScope.launch {
            try {
                val map = withContext(Dispatchers.IO) {
                    val api = Tvh.apiFor(server)
                    try { Tvh.fetchEpgGrid(server, api, force) } finally { api.close() }
                }
                if (map.isNotEmpty()) _epg.value = map
                loadedOnce = true
            } catch (_: Exception) {
            } finally {
                _loading.value = false
            }
        }
    }
}
