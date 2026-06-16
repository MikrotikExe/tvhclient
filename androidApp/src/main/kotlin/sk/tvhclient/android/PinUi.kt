package sk.tvhclient.android

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * PIN dialog (4 cislice). Zachytava cislice z dialkoveho ovladaca aj ma
 * dotykovu numericku klavesnicu. [onComplete] dostane zadane 4 cislice a vrati
 * true ak su prijate (dialog sa zavrie), alebo false ak su nespravne (zobrazi
 * chybu a vynuluje zadanie).
 */
@Composable
fun PinDialog(
    title: String,
    onDismiss: () -> Unit,
    onComplete: (String) -> Boolean
) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }

    fun push(d: Int) {
        if (entered.length >= 4) return
        entered += d
        error = false
        if (entered.length == 4) {
            val pin = entered
            if (onComplete(pin)) { /* zavrie volajuci */ }
            else { error = true; entered = "" }
        }
    }
    fun del() { if (entered.isNotEmpty()) entered = entered.dropLast(1) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
            Column(
                Modifier
                    .padding(24.dp)
                    .focusRequester(fr)
                    .focusable()
                    .onPreviewKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val kc = e.nativeKeyEvent.keyCode
                        val digit = when (kc) {
                            in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9 ->
                                kc - android.view.KeyEvent.KEYCODE_0
                            in android.view.KeyEvent.KEYCODE_NUMPAD_0..android.view.KeyEvent.KEYCODE_NUMPAD_9 ->
                                kc - android.view.KeyEvent.KEYCODE_NUMPAD_0
                            else -> -1
                        }
                        when {
                            digit >= 0 -> { push(digit); true }
                            kc == android.view.KeyEvent.KEYCODE_DEL -> { del(); true }
                            else -> false
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(4) { i ->
                        Box(
                            Modifier.size(18.dp).clip(CircleShape).background(
                                if (i < entered.length) MaterialTheme.colorScheme.primary
                                else Color(0x44FFFFFF)
                            )
                        )
                    }
                }
                if (error) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.plock_wrong), color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                // Dotykova numericka klavesnica (1-9, 0, zmazat)
                val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
                rows.forEach { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        r.forEach { d ->
                            OutlinedButton(onClick = { push(d) }, modifier = Modifier.width(64.dp)) {
                                Text("$d", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { del() }, modifier = Modifier.width(64.dp)) { Text("\u232B") }
                    OutlinedButton(onClick = { push(0) }, modifier = Modifier.width(64.dp)) {
                        Text("0", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.width(64.dp)) { Text("\u2715") }
                }
            }
        }
    }
}
