package dev.textmate.theme

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ThemeTest {

    private lateinit var darkPlus: Theme

    @Before
    fun setUp() {
        val darkVsStream = javaClass.classLoader.getResourceAsStream("themes/dark_vs.json")
            ?: throw IllegalArgumentException("dark_vs.json not found")
        val darkPlusStream = javaClass.classLoader.getResourceAsStream("themes/dark_plus.json")
            ?: throw IllegalArgumentException("dark_plus.json not found")
        darkPlus = darkVsStream.use { vs ->
            darkPlusStream.use { plus ->
                ThemeReader.readTheme(vs, plus)
            }
        }
    }

    // --- matchesScope unit tests ---

    @Test
    fun `matchesScope exact match`() {
        assertTrue(matchesScope("keyword", "keyword"))
    }

    @Test
    fun `matchesScope prefix with dot`() {
        assertTrue(matchesScope("keyword.control", "keyword"))
        assertTrue(matchesScope("keyword.control.kotlin", "keyword"))
        assertTrue(matchesScope("keyword.control.kotlin", "keyword.control"))
    }

    @Test
    fun `matchesScope no false prefix without dot`() {
        assertFalse(matchesScope("keywordx", "keyword"))
        assertFalse(matchesScope("keywordcontrol", "keyword"))
    }

    @Test
    fun `matchesScope pattern longer than scope`() {
        assertFalse(matchesScope("keyword", "keyword.control"))
    }

    // --- Theme.match tests with Dark+ ---

    @Test
    fun `keyword control matches dark_plus rule`() {
        val style = darkPlus.match(listOf("source.kotlin", "keyword.control.kotlin"))
        assertEquals(0xFFC586C0, style.foreground)
    }

    @Test
    fun `string matches dark_vs rule`() {
        val style = darkPlus.match(listOf("source.kotlin", "string.quoted.double.kotlin"))
        assertEquals(0xFFCE9178, style.foreground)
    }

    @Test
    fun `comment matches dark_vs rule`() {
        val style = darkPlus.match(listOf("source.kotlin", "comment.line.double-slash.kotlin"))
        assertEquals(0xFF608B4E, style.foreground)
    }

    @Test
    fun `keyword matches dark_vs rule`() {
        val style = darkPlus.match(listOf("source.kotlin", "keyword.other.kotlin"))
        assertEquals(0xFF569CD6, style.foreground)
    }

    @Test
    fun `keyword operator more specific in dark_vs`() {
        val style = darkPlus.match(listOf("source.kotlin", "keyword.operator.kotlin"))
        assertEquals(0xFFD4D4D4, style.foreground)
    }

    @Test
    fun `support type property-name matches dark_vs`() {
        val style = darkPlus.match(listOf("source.json", "support.type.property-name.json"))
        assertEquals(0xFF9CDCFE, style.foreground)
    }

    @Test
    fun `parent scope rule matches entity name function in object literal`() {
        // "meta.object-literal.key entity.name.function" → #9CDCFE
        val style = darkPlus.match(listOf("source.js", "meta.object-literal.key", "entity.name.function"))
        assertEquals(0xFF9CDCFE, style.foreground)
    }

    @Test
    fun `entity name function without matching parent uses base rule`() {
        // "entity.name.function" without parent → #DCDCAA
        val style = darkPlus.match(listOf("source.js", "entity.name.function"))
        assertEquals(0xFFDCDCAA, style.foreground)
    }

    @Test
    fun `unknown scope returns default`() {
        val style = darkPlus.match(listOf("source.kotlin", "some.unknown.scope"))
        assertEquals(darkPlus.defaultStyle.foreground, style.foreground)
    }

    @Test
    fun `empty scopes returns default style`() {
        val style = darkPlus.match(emptyList())
        assertEquals(darkPlus.defaultStyle, style)
    }

    // --- FontStyle tests ---

    @Test
    fun `emphasis resolves to italic`() {
        val style = darkPlus.match(listOf("text.html", "emphasis"))
        assertTrue(style.fontStyle.contains(FontStyle.ITALIC))
    }

    @Test
    fun `strong resolves to bold`() {
        val style = darkPlus.match(listOf("text.html", "strong"))
        assertTrue(style.fontStyle.contains(FontStyle.BOLD))
    }

    @Test
    fun `markup bold has bold fontStyle and foreground`() {
        val style = darkPlus.match(listOf("text.html.markdown", "markup.bold"))
        assertTrue(style.fontStyle.contains(FontStyle.BOLD))
        assertEquals(0xFF569CD6, style.foreground)
    }
}
