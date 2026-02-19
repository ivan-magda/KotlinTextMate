package dev.textmate.conformance

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
            arrayOf("JavaScript", "grammars/JavaScript.tmLanguage.json", "conformance/golden/javascript.snapshot.json"),
        )
    }

    @Test
    fun `tokens match golden snapshot`() {
        val rawGrammar = ConformanceTestSupport.loadRawGrammar(grammarResource)
        val grammar = ConformanceTestSupport.createGrammar(rawGrammar)
        val snapshot = ConformanceTestSupport.loadGoldenSnapshot(snapshotResource)

        for (file in snapshot.files) {
            ConformanceTestSupport.assertGrammarTokenization(
                grammar, file.lines, "$label/${file.source}"
            )
        }
    }
}
