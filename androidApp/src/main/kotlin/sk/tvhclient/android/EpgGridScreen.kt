package sk.tvhclient.android

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sk.tvhclient.shared.Tvh
import sk.tvhclient.shared.api.ChannelRow
import sk.tvhclient.shared.currentTimeSeconds
import sk.tvhclient.shared.formatDayLabel
import sk.tvhclient.shared.formatTimeHm
import sk.tvhclient.shared.model.EpgEvent

private const val PX_PER_MIN = 4          // sirka 1 minuty v dp
private const val HOUR_W = 60 * PX_PER_MIN // 240 dp
private const val DAY_MIN = 24 * 60
private const val PICON_COL = 64           // sirka stlpca s piconom
private const val ROW_H = 64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpgGridScreen(
    rows: List<ChannelRow>,
    seed: Map<String, List<EpgEvent>>,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val server = remember { Tvh.store.active() }
    val loader = remember(server?.id) { PiconImageLoader.get(context, server) }

    var dayOffset by remember { mutableStateOf(0) }
    val dayStart = remember(dayOffset) { dayStartSec(dayOffset) }
    val dayEnd = dayStart + DAY_MIN * 60
    // Tikajuci cas (live ciara a priebeh) — prekreslenie kazdych 30s
    var now by remember { mutableStateOf(currentTimeSeconds()) }
    LaunchedEffect(Unit) {
        while (true) { kotlinx.coroutines.delay(30_000); now = currentTimeSeconds() }
    }

    // Cache EPG per kanal (cele nacitane eventy); seed z now/next ak je
    val epg = remember { mutableStateMapOf<String, List<EpgEvent>>() }
    LaunchedEffect(seed) { seed.forEach { (k, v) -> if (epg[k] == null) epg[k] = v } }

    // DVR nahravky (minule relacie dozadu) — zdielana cache cez DvrViewModel
    // (prezije prepnutie kariet, nenacitava sa znova zo servera)
    val dvrVm: DvrViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val dvrState by dvrVm.state.collectAsState()
    LaunchedEffect(Unit) { dvrVm.loadIfNeeded() }
    val dvrByChannel = remember(dvrState) {
        (dvrState as? DvrState.Loaded)?.entries?.groupBy { it.channelName } ?: emptyMap()
    }

    val hScroll = rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Po otvoreni posun na aktualny cas (pre Dnes), s malym predstihom
    LaunchedEffect(dayOffset) {
        val nowMin = if (dayOffset == 0)
            (((currentTimeSeconds() - dayStart) / 60).toInt()) else 0
        val startMin = (nowMin - 30).coerceIn(0, DAY_MIN)
        val targetPx = with(density) { (startMin * PX_PER_MIN).dp.toPx() }.toInt()
        var tries = 0
        while (hScroll.maxValue == 0 && tries < 25) {
            kotlinx.coroutines.delay(20); tries++
        }
        hScroll.scrollTo(targetPx.coerceAtMost(hScroll.maxValue))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResourceTvGuide()) },
                navigationIcon = {
                    Text(
                        "  \u2190  ",
                        modifier = Modifier.padding(8.dp).clickable { onBack() },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    androidx.compose.material3.IconButton(onClick = { dvrVm.refresh() }) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.Refresh,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Vyber dna (minule -7 az +6); minule dni = nahravky z DVR
            val offsets = remember { (-7..6).toList() }
            val dayListState = androidx.compose.foundation.lazy.rememberLazyListState()
            LaunchedEffect(Unit) {
                val idx = offsets.indexOf(0).coerceAtLeast(0)
                dayListState.scrollToItem(idx)
            }
            LazyRow(
                state = dayListState,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(offsets) { off ->
                    val label = if (off == 0) "Dnes" else formatDayLabel(dayStartSec(off))
                    Box(Modifier.padding(end = 8.dp)) {
                        FilterChip(
                            selected = off == dayOffset,
                            onClick = { dayOffset = off },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Casova os (hlavicka) — skroluje horizontalne spolu s riadkami
            Row {
                Spacer(Modifier.width(PICON_COL.dp))
                Row(Modifier.horizontalScroll(hScroll)) {
                    for (h in 0 until 24) {
                        Text(
                            "%02d:00".format(h),
                            modifier = Modifier.width(HOUR_W.dp).padding(start = 4.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Riadky kanalov
            LazyColumn(Modifier.fillMaxSize()) {
                items(rows, key = { it.channel.uuid }) { row ->
                    val uuid = row.channel.uuid
                    // Lazy nacitanie EPG pre kanal (ak nie je v cache pre dany den)
                    val events = epg[uuid]
                    val hasDay = events?.any { it.stop > dayStart && it.start < dayEnd } == true
                    LaunchedEffect(uuid, dayOffset) {
                        if (!hasDay && server != null) {
                            try {
                                val list = withContext(Dispatchers.IO) {
                                    val api = Tvh.apiFor(server)
                                    try { Tvh.fetchEpgForChannel(server, api, uuid) } finally { api.close() }
                                }
                                val prev = epg[uuid] ?: emptyList()
                                epg[uuid] = (prev + list).distinctBy { it.eventId ?: it.start }
                            } catch (_: Exception) {
                            }
                        }
                    }
                    EpgGridRow(
                        row = row,
                        events = (epg[uuid] ?: emptyList()).filter { it.stop > dayStart && it.start < dayEnd },
                        dvr = (dvrByChannel[row.channel.name] ?: emptyList())
                            .filter { it.stop > dayStart && it.start < dayEnd },
                        dayStart = dayStart,
                        now = now,
                        showNow = dayOffset == 0,
                        hScroll = hScroll,
                        loader = loader,
                        onClick = { ev -> playLive(context, row, ev) },
                        onDvr = { e -> playDvr(context, e) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpgGridRow(
    row: ChannelRow,
    events: List<EpgEvent>,
    dvr: List<sk.tvhclient.shared.model.DvrEntry>,
    dayStart: Long,
    now: Long,
    showNow: Boolean,
    hScroll: androidx.compose.foundation.ScrollState,
    loader: coil.ImageLoader,
    onClick: (EpgEvent) -> Unit,
    onDvr: (sk.tvhclient.shared.model.DvrEntry) -> Unit
) {
    val context = LocalContext.current
    Row(Modifier.height(ROW_H.dp)) {
        // Picon stlpec (fixny)
        Box(
            Modifier.width(PICON_COL.dp).height(ROW_H.dp).padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (row.piconUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(row.piconUrl).build(),
                    contentDescription = row.channel.name,
                    imageLoader = loader,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    row.channel.name.take(3).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
        // Programova plocha (skroluje horizontalne)
        Box(
            Modifier
                .horizontalScroll(hScroll)
                .height(ROW_H.dp)
        ) {
            Box(Modifier.width((DAY_MIN * PX_PER_MIN).dp).height(ROW_H.dp)) {
                // Minule relacie z DVR (prehratelne dozadu) — len co skoncilo
                dvr.filter { it.stop <= now }.forEach { rec ->
                    GridBlock(
                        startMin = (((rec.start - dayStart) / 60).toInt()).coerceAtLeast(0),
                        endMin = (((rec.stop - dayStart) / 60).toInt()).coerceAtMost(DAY_MIN),
                        title = rec.title,
                        timeLabel = formatTimeHm(rec.start) + " - " + formatTimeHm(rec.stop),
                        bg = Color(0x3366BB6A),       // zelenkavy = nahrate, da sa prehrat
                        recorded = true,
                        onClick = { onDvr(rec) }
                    )
                }
                // Aktualne a buduce relacie z EPG
                events.filter { it.stop > now }.forEach { ev ->
                    val isNow = ev.start <= now && now < ev.stop
                    GridBlock(
                        startMin = (((ev.start - dayStart) / 60).toInt()).coerceAtLeast(0),
                        endMin = (((ev.stop - dayStart) / 60).toInt()).coerceAtMost(DAY_MIN),
                        title = ev.title.ifBlank { "—" },
                        timeLabel = formatTimeHm(ev.start) + " - " + formatTimeHm(ev.stop),
                        bg = if (isNow) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                             else Color(0x22FFFFFF),
                        recorded = false,
                        progressMin = if (isNow) ((now - ev.start) / 60).toInt() else 0,
                        onClick = { onClick(ev) }
                    )
                }
                // Zvisla live ciara aktualneho casu (jemna, ladi s farbou)
                if (showNow) {
                    val nowMin = ((now - dayStart) / 60).toInt()
                    if (nowMin in 0..DAY_MIN) {
                        Box(
                            Modifier
                                .offset(x = (nowMin * PX_PER_MIN).dp)
                                .width(1.5.dp)
                                .height(ROW_H.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridBlock(
    startMin: Int,
    endMin: Int,
    title: String,
    timeLabel: String,
    bg: Color,
    recorded: Boolean,
    progressMin: Int = 0,
    onClick: () -> Unit
) {
    val wMin = endMin - startMin
    if (wMin <= 0) return
    Box(
        Modifier
            .offset(x = (startMin * PX_PER_MIN).dp)
            .width((wMin * PX_PER_MIN).dp)
            .height(ROW_H.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .clickable { onClick() }
    ) {
        // Live priebeh (svetlejsia vypln zlava) pre bezzhiacu relaciu
        if (progressMin > 0) {
            Box(
                Modifier
                    .width((progressMin.coerceAtMost(wMin) * PX_PER_MIN).dp)
                    .height(ROW_H.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
            )
        }
        Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            Text(
                (if (recorded) "\u25B6 " else "") + title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private fun playLive(context: android.content.Context, row: ChannelRow, ev: EpgEvent) {
    val intent = android.content.Intent(context, PlayerActivity::class.java).apply {
        putExtra(PlayerActivity.EXTRA_UUID, row.channel.uuid)
        putExtra(PlayerActivity.EXTRA_TITLE, row.channel.name)
        putExtra(PlayerActivity.EXTRA_PROG_START, ev.start)
        putExtra(PlayerActivity.EXTRA_PROG_STOP, ev.stop)
        putExtra(PlayerActivity.EXTRA_PROG_TITLE, ev.title)
    }
    context.startActivity(intent)
}

/** Spatne prehratie minulej relacie z DVR nahravky (vratane resume/pozicie). */
private fun playDvr(context: android.content.Context, rec: sk.tvhclient.shared.model.DvrEntry) {
    val server = Tvh.store.active() ?: return
    val url = Tvh.dvrUrl(server, rec.uuid)
    val intent = android.content.Intent(context, PlayerActivity::class.java).apply {
        putExtra(PlayerActivity.EXTRA_URL, url)
        putExtra(PlayerActivity.EXTRA_TITLE, rec.title)
        putExtra(PlayerActivity.EXTRA_DURATION_MS, rec.durationSec * 1000)
        putExtra(PlayerActivity.EXTRA_DVR_UUID, rec.uuid)
    }
    context.startActivity(intent)
}

/** Zaciatok dna (lokalna polnoc) + offset dni, v sekundach. */
private fun dayStartSec(offset: Int): Long {
    val c = java.util.Calendar.getInstance()
    c.add(java.util.Calendar.DAY_OF_YEAR, offset)
    c.set(java.util.Calendar.HOUR_OF_DAY, 0)
    c.set(java.util.Calendar.MINUTE, 0)
    c.set(java.util.Calendar.SECOND, 0)
    c.set(java.util.Calendar.MILLISECOND, 0)
    return c.timeInMillis / 1000
}

@Composable
private fun stringResourceTvGuide(): String =
    androidx.compose.ui.res.stringResource(R.string.tv_guide)
