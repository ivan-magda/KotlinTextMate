package dev.textmate.conformance

import dev.textmate.grammar.tokenize.StateStack
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FirstMateConformanceTest(
    private val desc: String,
    private val testCase: FirstMateTestCase
) {

    companion object {
        private val SKIP_INJECTIONS = setOf("TEST #47", "TEST #49")
        private const val FIXTURES_BASE = "conformance/first-mate/"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun loadTestCases(): List<Array<Any>> {
            val allTests = ConformanceTestSupport.loadFirstMateTests(
                "${FIXTURES_BASE}tests.json"
            )
            return allTests
                .filter { it.desc !in SKIP_INJECTIONS }
                .filter { it.grammarInjections.isNullOrEmpty() }
                .filter { canRun(it) }
                .map { arrayOf(it.desc as Any, it as Any) }
        }

        // Lazily built map: scopeName -> resource path (only for available grammars)
        private val scopeToResource: Map<String, String> by lazy {
            val cl = javaClass.classLoader
            val allPaths = ConformanceTestSupport.loadFirstMateTests("${FIXTURES_BASE}tests.json")
                .flatMap { it.grammars }
                .distinct()
            allPaths.mapNotNull { path ->
                val resource = "$FIXTURES_BASE$path"
                if (cl.getResource(resource) != null) {
                    val raw = ConformanceTestSupport.loadRawGrammar(resource)
                    raw.scopeName to resource
                } else null
            }.toMap()
        }

        private fun canRun(test: FirstMateTestCase): Boolean {
            if (test.grammarPath != null) {
                return javaClass.classLoader.getResource("$FIXTURES_BASE${test.grammarPath}") != null
            }
            val scope = test.grammarScopeName ?: return false
            return scope in scopeToResource
        }
    }

    @Test
    fun `tokens match reference`() {
        val grammar = loadGrammarForTest()
        var state: StateStack? = null

        for ((lineIndex, expectedLine) in testCase.lines.withIndex()) {
            val result = grammar.tokenizeLine(expectedLine.line, state)
            val actual = ConformanceTestSupport.actualToExpected(
                expectedLine.line, result.tokens
            )

            ConformanceTestSupport.assertTokensMatch(
                lineText = expectedLine.line,
                lineIndex = lineIndex,
                expected = expectedLine.tokens,
                actual = actual,
                testDesc = desc
            )

            state = result.ruleStack
        }
    }

    private fun loadGrammarForTest(): dev.textmate.grammar.Grammar {
        val rawGrammars = testCase.grammars
            .filter { path ->
                javaClass.classLoader.getResource("${FIXTURES_BASE}$path") != null
            }
            .associate { path ->
                val raw = ConformanceTestSupport.loadRawGrammar("$FIXTURES_BASE$path")
                raw.scopeName to raw
            }

        val targetScope = when {
            testCase.grammarScopeName != null -> testCase.grammarScopeName
            testCase.grammarPath != null -> {
                val raw = ConformanceTestSupport.loadRawGrammar(
                    "$FIXTURES_BASE${testCase.grammarPath}"
                )
                raw.scopeName
            }
            else -> error("Test '${testCase.desc}' has neither grammarPath nor grammarScopeName")
        }

        val rawGrammar = rawGrammars[targetScope]
            ?: error("Grammar for scope '$targetScope' not found in: ${rawGrammars.keys}")

        return ConformanceTestSupport.createGrammar(rawGrammar)
    }
}
