package sk.tvhclient.android

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sk.tvhclient.shared.Tvh
import sk.tvhclient.shared.dateKey
import sk.tvhclient.shared.formatDateFull
import sk.tvhclient.shared.formatTimeHm
import sk.tvhclient.shared.model.DvrClassifier
import sk.tvhclient.shared.model.DvrEntry

// Navigacia v archive (read-only zlozky)
private sealed class DvrNav {
    data object Root : DvrNav()
    data object Channels : DvrNav()
    data class Dates(val channel: String) : DvrNav()
    data class Day(val channel: String, val dateKey: String) : DvrNav()
    data class Category(val catKey: String) : DvrNav()
}

@Composable
fun DvrScreen(vm: DvrViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var nav by remember { mutableStateOf<DvrNav>(DvrNav.Root) }

    LaunchedEffect(Unit) { vm.load() }

    // Spat: ak nie sme v root, vrat sa o uroven vyssie (nie zavri appku)
    BackHandler(enabled = nav != DvrNav.Root) {
        nav = when (val n = nav) {
            is DvrNav.Day -> DvrNav.Dates(n.channel)
            is DvrNav.Dates -> DvrNav.Channels
            is DvrNav.Channels -> DvrNav.Root
            is DvrNav.Category -> DvrNav.Root
            else -> DvrNav.Root
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is DvrState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is DvrState.NoServer -> Text(
                stringResource(R.string.no_active_server), Modifier.align(Alignment.Center)
            )
            is DvrState.Error -> Text(
                s.message, Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.error
            )
            is DvrState.Loaded -> {
                if (s.entries.isEmpty()) {
                    Text(stringResource(R.string.dvr_empty), Modifier.align(Alignment.Center))
                } else {
                    DvrContent(s.entries, s.channelOrder, nav, context, onNav = { nav = it })
                }
            }
        }
    }
}

@Composable
private fun DvrContent(
    entries: List<DvrEntry>,
    channelOrder: Map<String, Int>,
    nav: DvrNav,
    context: Context,
    onNav: (DvrNav) -> Unit
) {
    when (nav) {
        is DvrNav.Root -> {
            // Zlozky: Podla kanalov + kategorie ktore maju nahravky
            val byCat = entries.groupBy { DvrClassifier.classify(it) }
            LazyColumn(Modifier.fillMaxSize()) {
                item("hdr") { Header(stringResource(R.string.dvr_archive)) }
                item("by_channel") {
                    FolderRow(
                        "\uD83D\uDCFA  " + stringResource(R.string.dvr_by_channel),
                        sub = "${entries.map { it.channelName }.distinct().size} " +
                            stringResource(R.string.dvr_channels_count)
                    ) { onNav(DvrNav.Channels) }
                }
                item("cat_hdr") { Header(stringResource(R.string.dvr_by_genre)) }
                items(DvrClassifier.order.filter { byCat.containsKey(it) }, key = { it }) { cat ->
                    val cnt = byCat[cat]?.size ?: 0
                    FolderRow(
                        "\uD83D\uDCC1  " + catLabel(cat),
                        sub = "$cnt"
                    ) { onNav(DvrNav.Category(cat)) }
                }
            }
        }

        is DvrNav.Channels -> {
            // Kanaly ktore maju nahravky, zoradene podla cisla kanala (ako v zozname),
            // kanaly bez cisla na koniec podla abecedy.
            val byChannel = entries.groupBy { it.channelName.ifBlank { "—" } }
            val channels = byChannel.keys.sortedWith(
                compareBy({ channelOrder[it] ?: Int.MAX_VALUE }, { it.lowercase() })
            )
            LazyColumn(Modifier.fillMaxSize()) {
                item("hdr") { Header(stringResource(R.string.dvr_by_channel)) }
                items(channels, key = { it }) { ch ->
                    val cnt = byChannel[ch]?.size ?: 0
                    FolderRow("\uD83D\uDCFA  $ch", sub = "$cnt") {
                        onNav(DvrNav.Dates(ch))
                    }
                }
            }
        }

        is DvrNav.Dates -> {
            val chEntries = entries.filter { it.channelName.ifBlank { "—" } == nav.channel }
            val byDate = chEntries.groupBy { dateKey(it.start) }
            val dates = byDate.keys.sortedDescending()
            LazyColumn(Modifier.fillMaxSize()) {
                item("hdr") { Header(nav.channel) }
                items(dates, key = { it }) { dk ->
                    val list = byDate[dk] ?: emptyList()
                    val label = list.firstOrNull()?.let { formatDateFull(it.start) } ?: dk
                    FolderRow("\uD83D\uDCC5  $label", sub = "${list.size}") {
                        onNav(DvrNav.Day(nav.channel, dk))
                    }
                }
            }
        }

        is DvrNav.Day -> {
            val list = entries
                .filter { it.channelName.ifBlank { "—" } == nav.channel && dateKey(it.start) == nav.dateKey }
                .sortedByDescending { it.start }
            RecordingList(list, context, header = nav.channel)
        }

        is DvrNav.Category -> {
            val list = entries
                .filter { DvrClassifier.classify(it) == nav.catKey }
                .sortedByDescending { it.start }
            RecordingList(list, context, header = catLabel(nav.catKey))
        }
    }
}

@Composable
private fun RecordingList(list: List<DvrEntry>, context: Context, header: String) {
    LazyColumn(Modifier.fillMaxSize()) {
        item("hdr") { Header(header) }
        items(list, key = { it.uuid }) { entry ->
            RecordingRow(entry, context)
        }
    }
}

@Composable
private fun RecordingRow(entry: DvrEntry, context: Context) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                val server = Tvh.store.active() ?: return@clickable
                val url = Tvh.dvrUrl(server, entry.uuid)
                val intent = Intent(context, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_URL, url)
                    putExtra(PlayerActivity.EXTRA_TITLE, entry.title)
                    putExtra(PlayerActivity.EXTRA_DURATION_MS, entry.durationSec * 1000)
                }
                context.startActivity(intent)
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.titleSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = buildString {
                if (entry.channelName.isNotBlank()) append(entry.channelName)
                if (entry.start > 0) {
                    if (isNotEmpty()) append("  ·  ")
                    append(formatTimeHm(entry.start))
                }
                val mins = entry.durationSec / 60
                if (mins > 0) {
                    if (isNotEmpty()) append("  ·  ")
                    append("$mins min")
                }
            }
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text("\u25B6", Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun FolderRow(label: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(8.dp))
        Text(sub, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("  \u203A", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun catLabel(key: String): String {
    val resId = when (key) {
        DvrClassifier.FILM -> R.string.cat_film
        DvrClassifier.SERIAL -> R.string.cat_serial
        DvrClassifier.SPORT -> R.string.cat_sport
        DvrClassifier.NEWS -> R.string.cat_news
        DvrClassifier.SHOW -> R.string.cat_show
        DvrClassifier.CHILDREN -> R.string.cat_children
        DvrClassifier.MUSIC -> R.string.cat_music
        DvrClassifier.ARTS -> R.string.cat_arts
        DvrClassifier.DOCUMENTARY -> R.string.cat_documentary
        DvrClassifier.HOBBY -> R.string.cat_hobby
        else -> R.string.cat_other
    }
    return stringResource(resId)
}
