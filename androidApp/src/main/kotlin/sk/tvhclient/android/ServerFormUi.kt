package sk.tvhclient.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import sk.tvhclient.shared.api.ConnectionResult
import sk.tvhclient.shared.model.TvhServer

// UI servera: zoznam-riadok, formular pridania/upravy, vysledok testu, rozbalovacie pole
// (vyclenene z MainActivity.kt kvoli prehladnosti).

@Composable
fun ServerRow(
    server: TvhServer,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(server.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${server.host}:${server.port}" +
                            if (server.useHttps) " (HTTPS)" else "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isActive) {
                    Text(
                        stringResource(R.string.active),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isActive) {
                    TextButton(onClick = onSelect) { Text(stringResource(R.string.use_server)) }
                }
                TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerForm(vm: ServersViewModel, existing: TvhServer?, onClose: () -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var host by remember { mutableStateOf(existing?.host ?: "") }
    var port by remember { mutableStateOf((existing?.port ?: 9981).toString()) }
    var useHttps by remember { mutableStateOf(existing?.useHttps ?: false) }
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var password by remember { mutableStateOf(existing?.password ?: "") }
    var profile by remember { mutableStateOf(existing?.profile ?: "pass") }
    var authMode by remember { mutableStateOf(existing?.authMode ?: "auto") }
    var connMode by remember { mutableStateOf(existing?.connectionMode ?: "http") }
    var htspPort by remember { mutableStateOf((existing?.htspPort ?: 9982).toString()) }

    val testState by vm.testState.collectAsState()

    fun buildServer(): TvhServer? {
        val p = port.toIntOrNull() ?: return null
        if (host.isBlank()) return null
        return TvhServer(
            id = existing?.id ?: vm.newId(),
            name = name.ifBlank { host },
            host = host.trim(),
            port = p,
            useHttps = useHttps,
            username = username.trim(),
            password = password,
            profile = profile.trim().ifBlank { "pass" },
            authMode = authMode,
            connectionMode = connMode,
            htspPort = htspPort.toIntOrNull() ?: 9982
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    stringResource(
                        if (existing == null) R.string.add_server else R.string.edit_server
                    )
                )
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TvTextField(
                label = stringResource(R.string.field_name),
                value = name, onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth()
            )
            TvTextField(
                label = stringResource(R.string.field_host),
                value = host, onValueChange = { host = it },
                modifier = Modifier.fillMaxWidth()
            )
            TvTextField(
                label = stringResource(R.string.field_port),
                value = port, onValueChange = { port = it.filter(Char::isDigit) },
                modifier = Modifier.width(160.dp), numeric = true
            )
            DropdownField(
                label = stringResource(R.string.field_conn_mode),
                value = connMode,
                options = listOf("http", "htsp"),
                optionLabel = {
                    if (it == "htsp") stringResource(R.string.conn_htsp)
                    else stringResource(R.string.conn_http)
                },
                onSelect = { connMode = it }
            )
            if (connMode == "htsp") {
                TvTextField(
                    label = stringResource(R.string.field_htsp_port),
                    value = htspPort, onValueChange = { htspPort = it.filter(Char::isDigit) },
                    modifier = Modifier.width(160.dp), numeric = true
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = useHttps, onCheckedChange = { useHttps = it })
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.field_https))
            }
            TvTextField(
                label = stringResource(R.string.field_username),
                value = username, onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth()
            )
            TvTextField(
                label = stringResource(R.string.field_password),
                value = password, onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(), password = true
            )
            DropdownField(
                label = stringResource(R.string.field_profile),
                value = profile,
                options = listOf(
                    "pass", "mpegts", "matroska",
                    "webtv-h264-aac-matroska", "webtv-h264-aac-mpegts"
                ),
                optionLabel = { it },
                onSelect = { profile = it }
            )
            DropdownField(
                label = stringResource(R.string.field_auth),
                value = authMode,
                options = listOf("auto", "basic", "digest", "none"),
                optionLabel = {
                    when (it) {
                        "auto" -> stringResource(R.string.auth_auto)
                        "basic" -> stringResource(R.string.auth_basic)
                        "digest" -> stringResource(R.string.auth_digest)
                        else -> stringResource(R.string.auth_none)
                    }
                },
                onSelect = { authMode = it }
            )

            TestResultView(testState)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { buildServer()?.let(vm::test) },
                    enabled = testState !is TestState.Running
                ) {
                    Text(stringResource(R.string.test_connection))
                }
                Button(onClick = { buildServer()?.let { vm.save(it); onClose() } }) {
                    Text(stringResource(R.string.save))
                }
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
fun TestResultView(state: TestState) {
    when (state) {
        is TestState.Idle -> {}
        is TestState.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.testing))
        }
        is TestState.Done -> when (val r = state.result) {
            is ConnectionResult.Success -> Text(
                stringResource(
                    R.string.test_ok,
                    r.info.swVersion ?: "?",
                    r.info.apiVersion ?: 0
                ),
                color = MaterialTheme.colorScheme.primary
            )
            is ConnectionResult.AuthFailed -> Text(
                stringResource(R.string.test_auth_failed),
                color = MaterialTheme.colorScheme.error
            )
            is ConnectionResult.HttpError -> Text(
                stringResource(R.string.test_http_error, r.httpCode),
                color = MaterialTheme.colorScheme.error
            )
            is ConnectionResult.NetworkError -> Text(
                stringResource(R.string.test_network_error, r.message),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    optionLabel: @Composable (String) -> String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = optionLabel(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(optionLabel(opt)) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}
