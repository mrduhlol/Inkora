package com.abhishek.inkora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abhishek.inkora.domain.model.BlockKind
import com.abhishek.inkora.domain.model.ParaAlign
import com.abhishek.inkora.domain.model.SpanKind

/**
 * Two-tier formatting toolbar. The primary row holds the everyday tools
 * (undo, redo, B, I, U, S, bullet, numbered, checklist, More). The expandable
 * secondary panel holds advanced controls (heading, alignment, quote, code,
 * link, table, image, draw, divider, indent) so the bar stays compact above
 * the keyboard.
 */
@Composable
fun FormattingToolbar(
    active: Set<SpanKind>,
    blocks: Set<BlockKind> = emptySet(),
    align: ParaAlign = ParaAlign.LEFT,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrike: () -> Unit,
    onBullet: () -> Unit,
    onNumbered: () -> Unit,
    onChecklist: () -> Unit,
    onHeading: () -> Unit = {},
    onQuote: () -> Unit = {},
    onCode: () -> Unit = {},
    onLink: () -> Unit = {},
    onTable: () -> Unit = {},
    onImage: () -> Unit = {},
    onDraw: () -> Unit = {},
    onDivider: () -> Unit = {},
    onAlign: () -> Unit = {},
    onIndentMore: () -> Unit = {},
    onIndentLess: () -> Unit = {},
    fontSize: Int? = null,
    baseFontSize: Int = 16,
    onShrink: () -> Unit = {},
    onGrow: () -> Unit = {},
    fontColor: Int? = null,
    onColor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(modifier.fillMaxWidth(), tonalElevation = 2.dp) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ToolButton("Undo", enabled = canUndo, onClick = onUndo) {
                    Icon(Icons.Filled.Undo, null)
                }
                ToolButton("Redo", enabled = canRedo, onClick = onRedo) {
                    Icon(Icons.Filled.Redo, null)
                }
                ToolButton("Bold", active = active.contains(SpanKind.BOLD), onClick = onBold) {
                    Icon(Icons.Filled.FormatBold, null)
                }
                ToolButton("Italic", active = active.contains(SpanKind.ITALIC), onClick = onItalic) {
                    Icon(Icons.Filled.FormatItalic, null)
                }
                ToolButton("Underline", active = active.contains(SpanKind.UNDERLINE), onClick = onUnderline) {
                    Icon(Icons.Filled.FormatUnderlined, null)
                }
                ToolButton("Strikethrough", active = active.contains(SpanKind.STRIKE), onClick = onStrike) {
                    Icon(Icons.Filled.FormatStrikethrough, null)
                }
                ToolButton("Bullet list", active = blocks.contains(BlockKind.BULLET), onClick = onBullet) {
                    Icon(Icons.AutoMirrored.Filled.FormatListBulleted, null)
                }
                ToolButton("Numbered list", active = blocks.contains(BlockKind.NUMBERED), onClick = onNumbered) {
                    Icon(Icons.Filled.FormatListNumbered, null)
                }
                ToolButton("Checklist", active = blocks.contains(BlockKind.CHECK), onClick = onChecklist) {
                    Icon(Icons.Filled.CheckBox, null)
                }
                ToolButton(
                    if (expanded) "Fewer formatting options" else "More formatting options",
                    active = expanded,
                    onClick = { expanded = !expanded }
                ) {
                    Icon(Icons.Filled.MoreHoriz, null)
                }
            }
            if (expanded) {
                val headingActive = blocks.any {
                    it == BlockKind.HEADING1 || it == BlockKind.HEADING2 || it == BlockKind.HEADING3
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ToolButton("Heading", active = headingActive, onClick = onHeading) {
                        Icon(Icons.Filled.Title, null)
                    }
                    ToolButton("Quote", active = blocks.contains(BlockKind.QUOTE), onClick = onQuote) {
                        Icon(Icons.Filled.FormatQuote, null)
                    }
                    ToolButton("Code block", active = blocks.contains(BlockKind.CODE), onClick = onCode) {
                        Icon(Icons.Filled.Code, null)
                    }
                    ToolButton("Link", onClick = onLink) {
                        Icon(Icons.Filled.Link, null)
                    }
                    ToolButton("Table", active = blocks.contains(BlockKind.TABLE), onClick = onTable) {
                        Icon(Icons.Filled.TableChart, null)
                    }
                    ToolButton("Insert image", onClick = onImage) {
                        Icon(Icons.Filled.AddPhotoAlternate, null)
                    }
                    ToolButton("Draw", onClick = onDraw) {
                        Icon(Icons.Filled.Brush, null)
                    }
                    ToolButton("Divider", onClick = onDivider) {
                        Icon(Icons.Filled.HorizontalRule, null)
                    }
                    val alignIcon = when (align) {
                        ParaAlign.CENTER -> Icons.Filled.FormatAlignCenter
                        ParaAlign.RIGHT -> Icons.Filled.FormatAlignRight
                        ParaAlign.JUSTIFY -> Icons.Filled.FormatAlignJustify
                        ParaAlign.LEFT -> Icons.Filled.FormatAlignLeft
                    }
                    ToolButton("Text alignment: ${align.name.lowercase()}", onClick = onAlign) {
                        Icon(alignIcon, null)
                    }
                    ToolButton("Decrease indent", onClick = onIndentLess) {
                        Icon(Icons.Filled.FormatIndentDecrease, null)
                    }
                    ToolButton("Increase indent", onClick = onIndentMore) {
                        Icon(Icons.Filled.FormatIndentIncrease, null)
                    }
                    ToolButton("Smaller text", onClick = onShrink) {
                        Icon(Icons.Filled.TextDecrease, null)
                    }
                    ToolButton(
                        "Text size ${fontSize ?: baseFontSize}",
                        active = fontSize != null,
                        onClick = onGrow
                    ) {
                        Text(
                            "${fontSize ?: baseFontSize}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    ToolButton("Larger text", onClick = onGrow) {
                        Icon(Icons.Filled.TextIncrease, null)
                    }
                    ToolButton(
                        "Text color" + (if (fontColor != null) ": custom" else ": default"),
                        active = fontColor != null,
                        onClick = onColor
                    ) {
                        if (fontColor != null) {
                            Box(
                                Modifier.size(20.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(fontColor))
                            )
                        } else {
                            Icon(Icons.Filled.FormatColorText, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    description: String,
    onClick: () -> Unit,
    active: Boolean = false,
    enabled: Boolean = true,
    icon: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = description },
        colors = if (active) IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) else IconButtonDefaults.iconButtonColors(),
        content = { icon() }
    )
}
