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
 * EPG pre mriezku - PROGRESIVNE per-kanal nacitanie: kanal sa stiahne az ked
 * je jeho riadok viditelny (cez ensureChannel), takze viditelne kanaly sa
 * objavia rychlo a dalsie pribudaju pri skrolovani. Vysledok sa drzi v cache
 * (Activity-scoped ViewModel), takze prepnutie kariet ani znovuotvorenie
 * mriezky uz nestahuje to iste zo servera.
 */
class EpgGridViewModel : ViewModel() {

    private val _epg = MutableStateFlow<Map<String, List<EpgEvent>>>(emptyMap())
    val epg: StateFlow<Map<String, List<EpgEvent>>> = _epg

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // generacia: po refresh sa zvysi a riadky znova spustia ensureChannel
    private val _gen = MutableStateFlow(0)
    val gen: StateFlow<Int> = _gen

    private val inFlight = HashSet<String>()

    /** Nacita EPG pre jeden kanal, ak ho este nemame (volane pri zobrazeni riadku). */
    fun ensureChannel(uuid: String) {
        if (_epg.value.containsKey(uuid) || inFlight.contains(uuid)) return
        val server = Tvh.store.active() ?: return
        inFlight.add(uuid)
        _loading.value = true
        viewModelScope.launch {
            try {
                val evs = withContext(Dispatchers.IO) {
                    val api = Tvh.apiFor(server)
                    try { Tvh.fetchEpgForChannel(server, api, uuid) } finally { api.close() }
                }
                _epg.value = _epg.value + (uuid to evs)
            } catch (_: Exception) {
            } finally {
                inFlight.remove(uuid)
                if (inFlight.isEmpty()) _loading.value = false
            }
        }
    }

    /** Vynutene obnovenie - zahodi cache, riadky sa nacitaju nanovo. */
    fun refresh() {
        _epg.value = emptyMap()
        inFlight.clear()
        _gen.value = _gen.value + 1
    }
}
