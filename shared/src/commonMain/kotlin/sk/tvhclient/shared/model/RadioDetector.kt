package sk.tvhclient.shared.model

/**
 * Rozpoznanie radia. TVH v channel/grid radio priamo neoznacuje; plugin
 * (_bouquet_tags.py _is_radio_by_tags) ho urcuje podla nazvov tagov kanala.
 * Kanal je radio, ak ma tag ktoreho nazov zodpoveda radio tokenu.
 */
object RadioDetector {
    private val radioTokens = listOf(
        "radio", "radia", "radia fm", "radio fm",
        "radiostanice", "radiostanica", "rozhlas"
    )

    private fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s.lowercase()) {
            sb.append(
                when (c) {
                    'á', 'ä', 'à', 'â' -> 'a'
                    'č', 'ç' -> 'c'
                    'ď' -> 'd'
                    'é', 'ě', 'è', 'ê' -> 'e'
                    'í', 'ì', 'î' -> 'i'
                    'ľ', 'ĺ' -> 'l'
                    'ň' -> 'n'
                    'ó', 'ô', 'ö' -> 'o'
                    'ŕ', 'ř' -> 'r'
                    'š', 'ś' -> 's'
                    'ť' -> 't'
                    'ú', 'ů', 'ü' -> 'u'
                    'ý' -> 'y'
                    'ž', 'ź' -> 'z'
                    else -> c
                }
            )
        }
        return sb.toString().trim()
    }

    /** Je kanal radio podla nazvov jeho tagov? */
    fun isRadio(tagNames: List<String>): Boolean {
        for (raw in tagNames) {
            val n = normalize(raw)
            if (n.isEmpty()) continue
            for (tok in radioTokens) {
                val t = normalize(tok)
                if (n == t || n.contains(t)) return true
            }
        }
        return false
    }
}
