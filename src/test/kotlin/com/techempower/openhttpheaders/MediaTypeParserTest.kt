package com.techempower.openhttpheaders

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class MediaTypeParserTest : FunSpec({
  context("should parse successfully") {
    data class TestCase(
        val header: String,
        val qValueKey: String,
        val expected: List<MediaType>
    )
    withData(
        nameFn = {
          "q-value key: \"${it.qValueKey}\", input: \"${it.header}\""
        },
        listOf(
            // Should support basic captures
            TestCase(
                header = "text/html",
                qValueKey = "q",
                expected = listOf(
                    MediaType(
                        type = "text",
                        subtype = "html"
                    )
                )
            ),
            // Should support q-value
            TestCase(
                header = "text/html;q=0.9",
                qValueKey = "q",
                expected = listOf(
                    MediaType(
                        type = "text",
                        subtype = "html",
                        quality = 0.9,
                        parameters = mapOf("q" to "0.9")
                    )
                )
            ),
            TestCase(
                header = "text/html;q=1.0",
                qValueKey = "q",
                expected = listOf(
                    MediaType(
                        type = "text",
                        subtype = "html",
                        quality = 1.0,
                        parameters = mapOf("q" to "1.0")
                    )
                )
            ),
            TestCase(
                header = "text/html;q=0.999",
                qValueKey = "q",
                expected = listOf(
                    MediaType(
                        type = "text",
                        subtype = "html",
                        quality = 0.999,
                        parameters = mapOf("q" to "0.999")
                    )
                )
            ),
            // Should support other q-value keys
            TestCase(
                header = "text/html;qs=0.9",
                qValueKey = "qs",
                expected = listOf(
                    MediaType(
                        type = "text",
                        subtype = "html",
                        quality = 0.9,
                        parameters = mapOf("qs" to "0.9")
                    )
                )
            ),
            TestCase(
                header = "text/html;q=0.9;qs=0.8",
                qValueKey = "qs",
                expected = listOf(
                    MediaType(
                        type = "text",
                        subtype = "html",
                        quality = 0.8,
                        parameters = mapOf("q" to "0.9", "qs" to "0.8")
                    )
                )
            ),
            // Should support multi-type captures
            TestCase(
                header = "text/html,text/xml;q=0.9",
                qValueKey = "q",
                expected = listOf(
                    MediaType(type = "text", subtype = "html"),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.9,
                        parameters = mapOf("q" to "0.9")
                    )
                )
            ),
            // Should support non q-value parameters
            TestCase(
                header = "text/html;f=d;q=0.8,text/xml;q=0.9",
                qValueKey = "q",
                expected = listOf(
                    MediaType(
                        type = "text",
                        subtype = "html",
                        quality = 0.8,
                        parameters = mapOf("f" to "d", "q" to "0.8")
                    ),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.9,
                        parameters = mapOf("q" to "0.9")
                    )
                )
            ),
            // Should respect quote rules (these are a few random variations)
            TestCase(
                header = "text/html;f=\"dog\",text/xml;q=0.9",
                qValueKey = "q",
                expected = listOf(
                    MediaType(
                        type = "text",
                        subtype = "html",
                        parameters = mapOf("f" to "dog")
                    ),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.9,
                        parameters = mapOf("q" to "0.9")
                    )
                )
            ),
            TestCase(
                header = "text/html;f=\"d,og\",text/xml;q=0.9",
                qValueKey = "q",
                expected = listOf(
                    MediaType(
                        type = "text",
                        subtype = "html",
                        parameters = mapOf("f" to "d,og")
                    ),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.9,
                        parameters = mapOf("q" to "0.9")
                    )
                )
            ),
            TestCase(
                header = "text/html;f=\"d,o\";q=0.8,text/xml;q=0.9",
                qValueKey = "q",
                expected = listOf(
                    MediaType(
                        type = "text",
                        subtype = "html",
                        quality = 0.8,
                        parameters = mapOf("f" to "d,o", "q" to "0.8")
                    ),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.9,
                        parameters = mapOf("q" to "0.9")
                    )
                )
            ),
            // Should respect proper whitespace rules
            TestCase(
                header = "text/html,text/xml;   \tq=0.9",
                qValueKey = "q",
                expected = listOf(
                    MediaType(type = "text", subtype = "html"),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.9,
                        parameters = mapOf("q" to "0.9")
                    )
                )
            ),
            TestCase(
                header = "application/xml; q=0.2, application/json; q=0.7",
                qValueKey = "q",
                expected = listOf(
                    MediaType(
                        type = "application",
                        subtype = "xml",
                        quality = 0.2,
                        parameters = mapOf("q" to "0.2")
                    ),
                    MediaType(
                        type = "application",
                        subtype = "json",
                        quality = 0.7,
                        parameters = mapOf("q" to "0.7")
                    )
                )
            ),
            // Nested parameters should not cause problems
            TestCase(
                header = "application/xml; abc=\"def=xyz\"; q=0.2",
                qValueKey = "q",
                expected = listOf(
                    MediaType(
                        type = "application",
                        subtype = "xml",
                        quality = 0.2,
                        parameters = mapOf(
                            "abc" to "def=xyz",
                            "q" to "0.2",
                        )
                    ),
                )
            ),
            // Escaped values should be unescaped
            TestCase(
                header = "application/xml; abc=\"x\\\"yz\"",
                qValueKey = "q",
                expected = listOf(
                    MediaType(
                        type = "application",
                        subtype = "xml",
                        quality = null,
                        parameters = mapOf(
                            "abc" to "x\"yz"
                        )
                    ),
                )
            ),
        )
    ) {
      MediaTypeParser(it.qValueKey).parse(it.header) shouldBe it.expected
    }
  }
  context("should fail to parse") {
    data class TestCase(
        val header: String,
        val qValueKey: String
    )
    withData(
        nameFn = {
          "q-value key: \"${it.qValueKey}\", input: \"${it.header}\""
        },
        listOf(
            // Should fail to parse improper placement of quotes
            TestCase(
                header = "text/html;f=\\\"d,og\\\",text/xml;q=0.9",
                qValueKey = "q"
            ),
            TestCase(
                header = "text/html;f=\\\"d\";q=0.8,text/xml;q=0.9",
                qValueKey = "q"
            ),
            TestCase(
                header = "text/html;f=\\\"d\\\";q=0.8,text/xml;q=0.9",
                qValueKey = "q"
            ),
            // Should fail to parse improper placement of whitespace
            TestCase(
                header = "text/html,text/xml;q=   \\t0.9",
                qValueKey = "q"
            ),
            // Should fail to parse excessive numbers after decimal point
            TestCase(
                header = "text/html,text/xml;q=0.9999",
                qValueKey = "q"
            )
        )
    ) {
      shouldThrow<ProcessingException> {
        MediaTypeParser(it.qValueKey).parse(it.header)
      }
    }
  }
})
