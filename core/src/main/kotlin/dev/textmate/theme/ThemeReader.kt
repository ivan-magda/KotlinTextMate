package dev.textmate.theme

import com.google.gson.Gson
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Reads VS Code color themes from JSON files and constructs [Theme] instances.
 */
object ThemeReader {

    private val gson: Gson = Gson()

    /**
     * Reads a single theme from an [InputStream].
     * Supports both modern (`tokenColors`) and legacy (`settings`) formats.
     * Supports JSONC (JSON with comments).
     */
    fun readTheme(inputStream: InputStream): Theme {
        return readTheme(listOf(inputStream))
    }

    /**
     * Reads and merges multiple themes (base + overlays) in order.
     * Earlier streams have lower priority; later streams override.
     */
    fun readTheme(vararg inputStreams: InputStream): Theme {
        require(inputStreams.isNotEmpty()) { "At least one theme InputStream is required" }
        return readTheme(inputStreams.toList())
    }

    private fun readTheme(inputStreams: List<InputStream>): Theme {
        var globalIndex = 0
        val allRules = mutableListOf<ParsedThemeRule>()
        var themeName = ""

        for (stream in inputStreams) {
            val raw = parseRawTheme(stream)
            if (raw.name != null) themeName = raw.name

            val settings = raw.tokenColors ?: raw.settings ?: continue

            for (setting in settings) {
                val style = setting.settings ?: continue
                val foreground = style.foreground?.let { parseHexColor(it) }
                val background = style.background?.let { parseHexColor(it) }
                val fontStyle = parseFontStyle(style.fontStyle)

                val scopes = parseScopeField(setting.scope)
                for (scopeStr in scopes) {
                    val parts = scopeStr.trim().split(" ")
                    val leaf = parts.last()
                    val parents = if (parts.size > 1) parts.dropLast(1).reversed() else null

                    allRules.add(
                        ParsedThemeRule(
                            scope = leaf,
                            parentScopes = parents,
                            index = globalIndex++,
                            fontStyle = fontStyle,
                            foreground = foreground,
                            background = background
                        )
                    )
                }
            }
        }

        // Extract default style from rules with empty scope
        var defaultFg = 0xFF000000L
        var defaultBg = 0xFFFFFFFFL
        var defaultFontStyle: Set<FontStyle> = emptySet()

        val contentRules = mutableListOf<ParsedThemeRule>()
        for (rule in allRules) {
            if (rule.scope.isEmpty()) {
                if (rule.foreground != null) defaultFg = rule.foreground
                if (rule.background != null) defaultBg = rule.background
                if (rule.fontStyle != null) defaultFontStyle = rule.fontStyle
            } else {
                contentRules.add(rule)
            }
        }

        val sorted = contentRules.sortedWith(::compareRules)

        return Theme(
            name = themeName,
            defaultStyle = ResolvedStyle(defaultFg, defaultBg, defaultFontStyle),
            rules = sorted
        )
    }

    private fun parseRawTheme(inputStream: InputStream): RawTheme {
        val text = InputStreamReader(inputStream, Charsets.UTF_8).readText()
        val json = stripJsonc(text)
        return gson.fromJson(json, RawTheme::class.java)
            ?: throw IllegalArgumentException("Failed to parse theme: empty or invalid JSON")
    }
}

/**
 * Strips JSONC extensions (single-line comments, block comments, trailing commas)
 * while preserving string contents.
 */
internal fun stripJsonc(text: String): String {
    val sb = StringBuilder(text.length)
    var i = 0
    var inString = false
    while (i < text.length) {
        val c = text[i]
        if (inString) {
            sb.append(c)
            if (c == '\\' && i + 1 < text.length) {
                sb.append(text[++i])
            } else if (c == '"') {
                inString = false
            }
        } else {
            when {
                c == '"' -> {
                    inString = true
                    sb.append(c)
                }
                c == '/' && i + 1 < text.length && text[i + 1] == '/' -> {
                    while (i < text.length && text[i] != '\n') i++
                    i-- // will be incremented at end of loop
                }
                c == '/' && i + 1 < text.length && text[i + 1] == '*' -> {
                    i += 2
                    while (i + 1 < text.length && !(text[i] == '*' && text[i + 1] == '/')) i++
                    i++ // skip past '/'
                }
                else -> sb.append(c)
            }
        }
        i++
    }
    return sb.toString().replace(Regex(",\\s*([\\]\\}])"), "$1")
}

internal fun parseScopeField(scope: Any?): List<String> {
    return when (scope) {
        null -> listOf("")
        is String -> scope.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("") }
        is List<*> -> scope.filterIsInstance<String>()
        else -> listOf("")
    }
}

internal fun parseHexColor(hex: String): Long? {
    if (!hex.startsWith("#")) return null
    val digits = hex.substring(1)
    return when (digits.length) {
        6 -> {
            val rgb = digits.toLongOrNull(16) ?: return null
            0xFF000000L or rgb
        }
        8 -> {
            val rrggbbaa = digits.toLongOrNull(16) ?: return null
            val rr = (rrggbbaa shr 24) and 0xFF
            val gg = (rrggbbaa shr 16) and 0xFF
            val bb = (rrggbbaa shr 8) and 0xFF
            val aa = rrggbbaa and 0xFF
            (aa shl 24) or (rr shl 16) or (gg shl 8) or bb
        }
        else -> null
    }
}

internal fun parseFontStyle(fontStyle: String?): Set<FontStyle>? {
    if (fontStyle == null) return null
    if (fontStyle.isBlank()) return emptySet()
    val result = mutableSetOf<FontStyle>()
    for (token in fontStyle.split(" ")) {
        when (token.lowercase()) {
            "italic" -> result.add(FontStyle.ITALIC)
            "bold" -> result.add(FontStyle.BOLD)
            "underline" -> result.add(FontStyle.UNDERLINE)
            "strikethrough" -> result.add(FontStyle.STRIKETHROUGH)
        }
    }
    return result
}
