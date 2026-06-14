package sk.tvhclient.android

import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun RadioScreen(vm: RadioViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val query by vm.query.collectAsState()
    val context = LocalContext.current
    val server = remember { Tvh.store.active() }
    val loader = remember(server?.id) { PiconImageLoader.get(context, server) }

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

        Box(Modifier.fillMaxSize()) {
            when (val s = state) {
                is RadioState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is RadioState.NoServer -> Text(
                    stringResource(R.string.no_active_server), Modifier.align(Alignment.Center)
                )
                is RadioState.Error -> Text(
                    s.message, Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
                is RadioState.Loaded -> {
                    val q = query.trim().lowercase()
                    val rows = if (q.isBlank()) s.rows
                               else s.rows.filter { it.channel.name.lowercase().contains(q) }
                    if (rows.isEmpty()) {
                        Text(stringResource(R.string.radio_empty), Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(rows, key = { it.channel.uuid }) { row ->
                                RadioRow(row, loader, context)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioRow(
    row: ChannelRow,
    loader: coil.ImageLoader,
    context: android.content.Context
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                val server = Tvh.store.active() ?: return@clickable
                val url = Tvh.liveUrl(server, row.channel.uuid, row.channel.name,
                    server.profile.ifBlank { "pass" })
                val intent = Intent(context, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_URL, url)
                    putExtra(PlayerActivity.EXTRA_TITLE, row.channel.name)
                }
                context.startActivity(intent)
            }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(width = 56.dp, height = 40.dp), contentAlignment = Alignment.Center) {
            if (row.piconUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(row.piconUrl).build(),
                    contentDescription = row.channel.name,
                    imageLoader = loader,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
            } else {
                Text("\uD83D\uDCFB")  // radio emoji
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            row.channel.number?.let {
                Text("$it", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Text(row.channel.name, style = MaterialTheme.typography.bodyLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
