package dev.textmate.conformance

import dev.textmate.regex.JoniOnigLib
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Asserts the exact number of regex patterns that fell back to the
 * never-matching sentinel due to Joni compilation failures.
 * Catches silent degradation from Joni or grammar updates.
 */
class SentinelPatternTest {

    @Test
    fun `JSON grammar has 0 sentinel patterns`() {
        val onigLib = JoniOnigLib()
        loadAndCompileGrammar("grammars/JSON.tmLanguage.json", onigLib)
        assertEquals("JSON should have no sentinel patterns", 0, onigLib.sentinelPatternCount)
    }

    @Test
    fun `Kotlin grammar has 0 sentinel patterns`() {
        val onigLib = JoniOnigLib()
        loadAndCompileGrammar("grammars/kotlin.tmLanguage.json", onigLib)
        assertEquals("Kotlin should have no sentinel patterns", 0, onigLib.sentinelPatternCount)
    }

    @Test
    fun `JavaScript grammar has 0 sentinel patterns`() {
        val onigLib = JoniOnigLib()
        loadAndCompileGrammar("grammars/JavaScript.tmLanguage.json", onigLib)
        assertEquals("JavaScript should have no sentinel patterns", 0, onigLib.sentinelPatternCount)
    }

    @Test
    fun `Markdown grammar has exactly 1 sentinel pattern`() {
        val onigLib = JoniOnigLib()
        loadAndCompileGrammar("grammars/markdown.tmLanguage.json", onigLib)
        assertEquals(
            "Markdown should have exactly 1 sentinel (strikethrough)",
            1,
            onigLib.sentinelPatternCount
        )
    }

    private fun loadAndCompileGrammar(resourcePath: String, onigLib: JoniOnigLib) {
        val rawGrammar = ConformanceTestSupport.loadRawGrammar(resourcePath)
        val grammar = dev.textmate.grammar.Grammar(
            rawGrammar.scopeName,
            rawGrammar,
            onigLib
        )
        // Tokenize representative text to trigger lazy pattern compilation.
        // Only rules actually entered get their sub-patterns compiled, so deeply
        // nested rules not exercised here won't be counted.
        val lines = listOf(
            "# heading",
            "some **bold** and ~~strike~~ text",
            "{ \"key\": true }",
            "fun main() { val x = 1 }"
        )
        var state: dev.textmate.grammar.tokenize.StateStack? = null
        for (line in lines) {
            state = grammar.tokenizeLine(line, state).ruleStack
        }
    }
}
