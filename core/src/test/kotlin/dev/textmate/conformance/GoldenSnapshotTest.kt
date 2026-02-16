package dev.textmate.conformance

import dev.textmate.grammar.tokenize.StateStack
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class GoldenSnapshotTest(
    private val label: String,
    private val grammarResource: String,
    private val snapshotResource: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun grammars(): List<Array<Any>> = listOf(
            arrayOf("JSON", "grammars/JSON.tmLanguage.json", "conformance/golden/json.snapshot.json"),
            arrayOf("Kotlin", "grammars/kotlin.tmLanguage.json", "conformance/golden/kotlin.snapshot.json"),
            arrayOf("Markdown", "grammars/markdown.tmLanguage.json", "conformance/golden/markdown.snapshot.json"),
        )
    }

    @Test
    fun `tokens match golden snapshot`() {
        val rawGrammar = ConformanceTestSupport.loadRawGrammar(grammarResource)
        val grammar = ConformanceTestSupport.createGrammar(rawGrammar)
        val snapshot = ConformanceTestSupport.loadGoldenSnapshot(snapshotResource)

        for (file in snapshot.files) {
            var state: StateStack? = null

            for ((lineIndex, expectedLine) in file.lines.withIndex()) {
                val result = grammar.tokenizeLine(expectedLine.line, state)
                val actual = ConformanceTestSupport.actualToExpected(
                    expectedLine.line, result.tokens
                )

                ConformanceTestSupport.assertTokensMatch(
                    lineText = expectedLine.line,
                    lineIndex = lineIndex,
                    expected = expectedLine.tokens,
                    actual = actual,
                    testDesc = "$label/${file.source}"
                )

                state = result.ruleStack
            }
        }
    }
}
