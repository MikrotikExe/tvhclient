package sk.tvhclient.android

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Text, ktory sa sam zmensi, aby sa zmestil do dostupnej sirky/vysky.
 * Urcene pre stiesnene UI (taby, tlacidla, cipy, titulky v uzkych riadkoch),
 * kde sa text na roznych rozliseniach/skalovani pisma inak lame alebo orezava.
 *
 * Bezny prozaicky text NECHAJ normalne zalamovat — toto nie je nahrada za Text vsade,
 * len pre miesta s pevne obmedzenou sirkou.
 *
 * Funguje aj na starsom Compose (bez natívneho autoSize): postupne znizuje fontSize,
 * kym sa obsah nezmesti, alebo kym nedosiahne minTextSize. Vykresli sa az ked je
 * velkost vyriesena (ziadne blikanie pocas merania).
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    minTextSize: TextUnit = 9.sp,
    style: TextStyle = LocalTextStyle.current
) {
    val base = if (style.fontSize == TextUnit.Unspecified) 14.sp else style.fontSize
    var scaled by remember(text, style, maxLines) { mutableStateOf(style.copy(fontSize = base)) }
    var ready by remember(text, style, maxLines) { mutableStateOf(false) }
    Text(
        text = text,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        color = color,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        overflow = TextOverflow.Clip,
        style = scaled,
        onTextLayout = { res ->
            if (!ready) {
                if (res.didOverflowWidth || res.didOverflowHeight) {
                    val next = scaled.fontSize * 0.92f
                    if (next.value >= minTextSize.value) scaled = scaled.copy(fontSize = next)
                    else ready = true
                } else {
                    ready = true
                }
            }
        }
    )
}
