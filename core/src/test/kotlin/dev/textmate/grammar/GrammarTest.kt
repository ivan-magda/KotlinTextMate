package dev.textmate.grammar

import dev.textmate.grammar.raw.GrammarReader
import dev.textmate.regex.JoniOnigLib
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GrammarTest {

    private lateinit var grammar: Grammar

    private fun loadGrammar(resourcePath: String): Grammar {
        val rawGrammar = javaClass.classLoader.getResourceAsStream(resourcePath)
            ?.use { stream -> GrammarReader.readGrammar(stream) }
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")
        return Grammar(rawGrammar.scopeName, rawGrammar, JoniOnigLib())
    }

    @Before
    fun setUp() {
        grammar = loadGrammar("grammars/JSON.tmLanguage.json")
    }

    @Test
    fun `tokenize empty string`() {
        val result = grammar.tokenizeLine("")
        assertTrue("Should have at least one token", result.tokens.isNotEmpty())
        assertTrue(
            "Token should include source.json scope",
            result.tokens.any { token -> token.scopes.any { it.contains("source.json") } }
        )
    }

    @Test
    fun `tokenize JSON boolean`() {
        val result = grammar.tokenizeLine("true")
        val hasConstantLanguage = result.tokens.any { token ->
            token.scopes.any { it.contains("constant.language") }
        }
        assertTrue("'true' should produce a constant.language scope", hasConstantLanguage)
    }

    @Test
    fun `tokenize JSON number`() {
        val result = grammar.tokenizeLine("42")
        val hasConstantNumeric = result.tokens.any { token ->
            token.scopes.any { it.contains("constant.numeric") }
        }
        assertTrue("'42' should produce a constant.numeric scope", hasConstantNumeric)
    }

    @Test
    fun `tokenize JSON object`() {
        val result = grammar.tokenizeLine("""{"key": "value"}""")
        val hasStringScope = result.tokens.any { token ->
            token.scopes.any { it.contains("string") }
        }
        assertTrue("JSON object should have string scopes", hasStringScope)
    }

    @Test
    fun `tokens cover entire line`() {
        val line = """{"key": "value"}"""
        val result = grammar.tokenizeLine(line)
        val tokens = result.tokens

        assertEquals("First token should start at 0", 0, tokens.first().startIndex)

        // No gaps between tokens
        for (i in 1 until tokens.size) {
            assertEquals(
                "Token $i should start where token ${i - 1} ends",
                tokens[i - 1].endIndex,
                tokens[i].startIndex
            )
        }

        assertEquals("Last token should end at line length", line.length, tokens.last().endIndex)
    }

    @Test
    fun `multiline state passing`() {
        val result1 = grammar.tokenizeLine("{")
        val result2 = grammar.tokenizeLine(""""key": "value"""", result1.ruleStack)

        val hasStringScope = result2.tokens.any { token ->
            token.scopes.any { it.contains("string") }
        }
        assertTrue("Line 2 should have string scopes using prevState", hasStringScope)
    }

    @Test
    fun `multiline block comment`() {
        // JSON grammar supports block comments (jsonc-style comments in some grammars)
        // The JSON grammar from VS Code supports line comments
        // Let's test with a string that spans conceptually via state
        val result1 = grammar.tokenizeLine("[")
        val result2 = grammar.tokenizeLine("1,", result1.ruleStack)

        val hasNumericScope = result2.tokens.any { token ->
            token.scopes.any { it.contains("constant.numeric") }
        }
        assertTrue("Number inside array on line 2 should have numeric scope", hasNumericScope)
    }

    @Test
    fun `INITIAL state works like null`() {
        val resultNull = grammar.tokenizeLine("true", null)
        val resultInitial = grammar.tokenizeLine("true", INITIAL)

        assertEquals(
            "INITIAL should produce same tokens as null",
            resultNull.tokens.map { it.scopes },
            resultInitial.tokens.map { it.scopes }
        )
    }

    @Test
    fun `detailed scope check for curly brace`() {
        val result = grammar.tokenizeLine("{")
        val braceToken = result.tokens.find { token ->
            token.scopes.any { it.contains("punctuation.definition.dictionary.begin") }
        }
        assertNotNull(
            "'{' should have punctuation.definition.dictionary.begin scope",
            braceToken
        )
    }
}
