package dev.textmate.conformance

import org.junit.Assert.*
import org.junit.Test

class ConformanceTestSupportTest {

    @Test
    fun `loads first-mate tests json`() {
        val tests = ConformanceTestSupport.loadFirstMateTests(
            "conformance/first-mate/tests.json"
        )
        assertTrue("Should load at least 60 test cases", tests.size >= 60)

        val test3 = requireNotNull(tests.find { it.desc == "TEST #3" }) { "Should find TEST #3" }
        assertEquals("fixtures/hello.json", test3.grammarPath)
        assertEquals(1, test3.lines.size)
        assertEquals("hello world!", test3.lines[0].line)
        assertTrue(test3.lines[0].tokens.isNotEmpty())

        val firstToken = test3.lines[0].tokens[0]
        assertEquals("hello", firstToken.value)
        assertTrue(firstToken.scopes.contains("source.hello"))
    }
}
