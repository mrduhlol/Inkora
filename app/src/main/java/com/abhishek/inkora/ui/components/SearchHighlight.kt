package com.abhishek.inkora.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/** Maximum highlighted occurrences per string: keeps large bodies cheap. */
const val MAX_HIGHLIGHTS = 8

/**
 * Case-insensitive, non-overlapping match ranges of [query] in [text].
 * Empty/blank queries yield no ranges. Pure and unit-tested.
 */
fun highlightRanges(text: String, query: String): List<IntRange> {
    val q = query.trim()
    if (q.isEmpty() || text.isEmpty()) return emptyList()
    val out = mutableListOf<IntRange>()
    var from = 0
    while (out.size < MAX_HIGHLIGHTS) {
        val i = text.indexOf(q, from, ignoreCase = true)
        if (i < 0) break
        out.add(i until i + q.length)
        from = i + q.length
    }
    return out
}

/**
 * [text] with query matches wearing a subtle tinted background — never a
 * harsh neon wash. Falls back to plain text when there is no query.
 */
@Composable
fun rememberHighlighted(
    text: String,
    query: String,
    highlightColor: Color = MaterialTheme.colorScheme.primary
): AnnotatedString {
    val ranges = remember(text, query) { highlightRanges(text, query) }
    return remember(text, ranges, highlightColor) {
        if (ranges.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                append(text)
                ranges.forEach { r ->
                    addStyle(
                        SpanStyle(background = highlightColor.copy(alpha = 0.22f)),
                        r.first,
                        r.last + 1
                    )
                }
            }
        }
    }
}
