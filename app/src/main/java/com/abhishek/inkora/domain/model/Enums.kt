package com.abhishek.inkora.domain.model

/**
 * Page background styles persisted per-note. Kept as String in Room so new
 * styles can be added without migration; this enum is the V1 closed set.
 */
enum class PageStyle(val key: String) {
    BLANK("blank"),
    RULED("ruled"),
    GRID("grid"),
    DOTTED("dotted");

    companion object {
        fun fromKey(key: String?): PageStyle =
            entries.firstOrNull { it.key == key } ?: BLANK
    }
}

/** Named paper backgrounds. CUSTOM allows a user-picked ARGB color. */
enum class PaperBackground(val key: String) {
    WHITE("white"),
    CREAM("cream"),
    GRAY("gray"),
    DARK("dark"),
    CUSTOM("custom");

    companion object {
        fun fromKey(key: String?): PaperBackground =
            entries.firstOrNull { it.key == key } ?: CREAM
    }
}

enum class AppTheme(val key: String) { SYSTEM("system"), LIGHT("light"), DARK("dark"), AMOLED("amoled") }
enum class AccentColor(val key: String) {
    BLUE("blue"), PURPLE("purple"), GREEN("green"), ORANGE("orange"),
    RED("red"), PINK("pink"), TEAL("teal")
}
enum class SortOrder(val key: String) {
    UPDATED_DESC("updated_desc"), CREATED_DESC("created_desc"),
    TITLE_ASC("title_asc"), TITLE_DESC("title_desc")
}

enum class HomeViewMode(val key: String) { GRID("grid"), LIST("list") }

enum class CardDensity(val key: String) { COMFORTABLE("comfortable"), COMPACT("compact") }

/** App-wide UI scale. Never touches note content — chrome only. */
enum class DisplaySize(val key: String) {
    SMALL("small"), DEFAULT("default"), LARGE("large");

    /** Layout multiplier applied to chrome dimensions. */
    val scale: Float
        get() = when (this) {
            SMALL -> 0.9f
            DEFAULT -> 1f
            LARGE -> 1.15f
        }

    companion object {
        fun fromKey(key: String?): DisplaySize =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/** Explicit note kind. Stored per-note; never inferred from content. */
enum class NoteType(val key: String) {
    TEXT("text"), HANDWRITING("handwriting"), TODO("todo");

    companion object {
        fun fromKey(key: String?): NoteType =
            entries.firstOrNull { it.key == key } ?: TEXT
    }
}
