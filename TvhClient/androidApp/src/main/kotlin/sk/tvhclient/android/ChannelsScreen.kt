package sk.tvhclient.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import sk.tvhclient.shared.Tvh
import sk.tvhclient.shared.api.ChannelRow

@Composable
fun ChannelsScreen(vm: ChannelsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val query by vm.query.collectAsState()
    var selectedTag by remember { mutableStateOf<String?>(null) } // tag uuid alebo null = vsetky

    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { vm.setQuery(it) },
            label = { Text(stringResource(R.string.search_channels)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        when (val s = state) {
            is ChannelsState.Loading -> CenterBox { CircularProgressIndicator() }
            is ChannelsState.NoServer -> CenterBox {
                Text(stringResource(R.string.no_active_server))
            }
            is ChannelsState.Error -> CenterBox {
                Text(s.message, color = MaterialTheme.colorScheme.error)
            }
            is ChannelsState.Loaded -> {
                if (query.isNotBlank()) {
                    // Vyhladavanie: plochy filtrovany zoznam
                    val q = query.trim().lowercase()
                    val results = s.allRows.filter { it.channel.name.lowercase().contains(q) }
                    ChannelList(results)
                } else {
                    // Filtre podla tagov
                    val tags = s.categories.mapNotNull { it.tag }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { selectedTag = null },
                                label = { Text(stringResource(R.string.all_channels)) }
                            )
                        }
                        items(tags, key = { it.uuid }) { tag ->
                            FilterChip(
                                selected = selectedTag == tag.uuid,
                                onClick = { selectedTag = tag.uuid },
                                label = { Text(tag.name) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val rows = if (selectedTag == null) {
                        s.categories.flatMap { it.rows }.distinctBy { it.channel.uuid }
                    } else {
                        s.categories.firstOrNull { it.tag?.uuid == selectedTag }?.rows ?: emptyList()
                    }
                    ChannelList(rows)
                }
            }
        }
    }
}

@Composable
private fun ChannelList(rows: List<ChannelRow>) {
    val context = LocalContext.current
    val server = remember { Tvh.store.active() }
    val loader = remember(server?.id) { PiconImageLoader.get(context, server) }

    if (rows.isEmpty()) {
        CenterBox { Text(stringResource(R.string.no_channels)) }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(rows, key = { it.channel.uuid }) { row ->
            ChannelItem(row, loader, context)
        }
    }
}

@Composable
private fun ChannelItem(
    row: ChannelRow,
    loader: coil.ImageLoader,
    context: android.content.Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { /* M3: spusti prehravanie */ }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp, 40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x22FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (row.piconUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(row.piconUrl).build(),
                    contentDescription = row.channel.name,
                    imageLoader = loader,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
            } else {
                Text(
                    row.channel.name.take(2).uppercase(),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (row.channel.number != null) {
                    Text(
                        "${row.channel.number}  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    row.channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val now = row.nowTitle
            if (now != null) {
                Text(
                    now,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
