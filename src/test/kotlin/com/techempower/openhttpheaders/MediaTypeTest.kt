package com.techempower.openhttpheaders

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe


class MediaTypeTest : FunSpec({
  context("matches") {
    data class TestCase(
        val namePrefix: String? = null,
        val a: MediaType,
        val b: MediaType,
        val matches: Boolean
    )
    context("without parameter matching") {
      withData(
          nameFn = {
            "${it.namePrefix ?: ""}${it.a}, ${it.b}, matches: ${it.matches}"
          },
          listOf(
              // Perfect match
              TestCase(
                  a = MediaType(type = "text", subtype = "html"),
                  b = MediaType(type = "text", subtype = "html"),
                  matches = true
              ),
              // One uses a wildcard for subtype
              TestCase(
                  a = MediaType(type = "text", subtype = "html"),
                  b = MediaType(type = "text", subtype = "*"),
                  matches = true
              ),
              // Both use a wildcard for subtype
              TestCase(
                  a = MediaType(type = "text", subtype = "*"),
                  b = MediaType(type = "text", subtype = "*"),
                  matches = true
              ),
              // Both use a wildcard for subtype, one uses wildcard for type
              TestCase(
                  a = MediaType(type = "text", subtype = "*"),
                  b = MediaType(type = "*", subtype = "*"),
                  matches = true
              ),
              // Both use a wildcard for type and subtype
              TestCase(
                  a = MediaType(type = "*", subtype = "*"),
                  b = MediaType(type = "*", subtype = "*"),
                  matches = true
              ),
              // Casing does not match in type
              TestCase(
                  namePrefix = "type test: ",
                  a = MediaType(type = "Text", subtype = "html"),
                  b = MediaType(type = "text", subtype = "html"),
                  matches = true
              ),
              // Casing does not match in subtype
              TestCase(
                  namePrefix = "subtype test: ",
                  a = MediaType(type = "text", subtype = "Html"),
                  b = MediaType(type = "text", subtype = "html"),
                  matches = true
              ),
              // Not a match, no wildcards
              TestCase(
                  a = MediaType(type = "text", subtype = "html"),
                  b = MediaType(type = "application", subtype = "json"),
                  matches = false
              ),
              // Not a match, one uses wildcard for subtype
              TestCase(
                  a = MediaType(type = "text", subtype = "html"),
                  b = MediaType(type = "application", subtype = "*"),
                  matches = false
              ),
              // Not a match, both use wildcard for subtype
              TestCase(
                  a = MediaType(type = "text", subtype = "*"),
                  b = MediaType(type = "application", subtype = "*"),
                  matches = false
              ),
              // Parameters should be ignored such that it matches
              TestCase(
                  a = MediaType(
                      type = "text",
                      subtype = "html",
                      parameters = mapOf("charset" to "UTF-8")
                  ),
                  b = MediaType(
                      type = "text",
                      subtype = "html",
                      parameters = mapOf("charset" to "UTF-16")
                  ),
                  matches = true
              )
          )
      ) {
        it.a.matches(it.b) shouldBe it.matches
        it.b.matches(it.a) shouldBe it.matches
      }
    }
    context("with parameter matching") {
      withData(
          nameFn = {
            "${it.a}, ${it.b}, matches: ${it.matches}"
          },
          listOf(
              TestCase(
                  a = MediaType(type = "text", subtype = "html", mapOf()),
                  b = MediaType(type = "text", subtype = "html", mapOf()),
                  matches = true
              ),
              TestCase(
                  a = MediaType(
                      type = "text",
                      subtype = "html",
                      mapOf("charset" to "UTF-8")
                  ),
                  b = MediaType(
                      type = "text",
                      subtype = "html",
                      mapOf()
                  ),
                  matches = true
              ),
              TestCase(
                  a = MediaType(
                      type = "text",
                      subtype = "html",
                      mapOf("charset" to "UTF-8")
                  ),
                  b = MediaType(
                      type = "text",
                      subtype = "html",
                      mapOf("charset" to "UTF-8")
                  ),
                  matches = true
              ),
              TestCase(
                  a = MediaType(
                      type = "text",
                      subtype = "html",
                      mapOf("charset" to "UTF-8")
                  ),
                  b = MediaType(
                      type = "text",
                      subtype = "html",
                      mapOf("charset" to "UTF-16")
                  ),
                  matches = false
              ),
              TestCase(
                  a = MediaType(
                      type = "text",
                      subtype = "html",
                      mapOf("charset" to "UTF-8", "test" to "case")
                  ),
                  b = MediaType(
                      type = "text",
                      subtype = "html",
                      mapOf("charset" to "UTF-8")
                  ),
                  matches = true
              ),
              TestCase(
                  a = MediaType(
                      type = "text",
                      subtype = "html",
                      mapOf("charset" to "UTF-8", "test" to "case")
                  ),
                  b = MediaType(
                      type = "text",
                      subtype = "html",
                      mapOf("charset" to "UTF-8", "test2" to "case2")
                  ),
                  matches = true
              ),
              TestCase(
                  a = MediaType(
                      type = "text",
                      subtype = "html",
                      mapOf("charset" to "UTF-8", "test" to "case")
                  ),
                  b = MediaType(
                      type = "text",
                      subtype = "html",
                      mapOf(
                          "charset" to "UTF-8",
                          "test" to "case2",
                          "test2" to "case2"
                      )
                  ),
                  matches = false
              )
          )
      ) {
        it.a.matches(it.b, checkParameters = true) shouldBe it.matches
        it.b.matches(it.a, checkParameters = true) shouldBe it.matches
      }
    }
  }
  context("constructor") {
    test("should reject quality values over 1") {
      shouldThrow<ProcessingException> {
        MediaType(
            type = "text",
            subtype = "html",
            quality = 1.001
        )
      }
    }
    test("should reject quality values under 0") {
      shouldThrow<ProcessingException> {
        MediaType(
            type = "text",
            subtype = "html",
            quality = -1.0
        )
      }
    }
    test("should reject quality values with more than 3 decimal places") {
      shouldThrow<ProcessingException> {
        MediaType(
            type = "text",
            subtype = "html",
            quality = 0.9999
        )
      }
    }
    test("should reject type-wildcard, subtype-defined combinations") {
      shouldThrow<ProcessingException> {
        MediaType(
            type = "*",
            subtype = "html"
        )
      }
    }
  }
  context("equals") {
    context("should be case-insensitive for type and subtype") {
      data class TestCase(val name: String, val a: MediaType, val b: MediaType)
      withData(
          nameFn = { it.name },
          listOf(
              TestCase(
                  name = "type",
                  a = MediaType(type = "text", subtype = "html"),
                  b = MediaType(type = "Text", subtype = "html")
              ),
              TestCase(
                  name = "subtype",
                  a = MediaType(type = "text", subtype = "Html"),
                  b = MediaType(type = "text", subtype = "html")
              )
          )
      ) {
        it.a shouldBe it.b
        it.b shouldBe it.a
      }
    }
  }
  context("hashCode") {
    context("should be case-insensitive for type and subtype") {
      data class TestCase(val name: String, val a: MediaType, val b: MediaType)
      withData(
          nameFn = { it.name },
          listOf(
              TestCase(
                  name = "type",
                  a = MediaType(type = "text", subtype = "html"),
                  b = MediaType(type = "Text", subtype = "html")
              ),
              TestCase(
                  name = "subtype",
                  a = MediaType(type = "text", subtype = "Html"),
                  b = MediaType(type = "text", subtype = "html")
              )
          )
      ) {
        it.a.hashCode() shouldBe it.b.hashCode()
      }
    }
  }
  context("QUALITY_VALUE_COMPARATOR") {
    data class TestCase(
        val name: String,
        val list: List<MediaType>,
        val expected: List<MediaType>
    )
    withData(
        nameFn = { it.name },
        listOf(
            TestCase(
                name = "should first sort by quality (nulls first)",
                list = listOf(
                    MediaType(type = "text", subtype = "html", quality = 0.5),
                    MediaType(type = "text", subtype = "xml", quality = null),
                    MediaType(type = "text", subtype = "html", quality = 1.0),
                ),
                expected = listOf(
                    MediaType(type = "text", subtype = "xml", quality = null),
                    MediaType(type = "text", subtype = "html", quality = 1.0),
                    MediaType(type = "text", subtype = "html", quality = 0.5),
                )
            ),
            TestCase(
                name = "should secondarily sort by type (alphabetical, case insensitive)",
                list = listOf(
                    MediaType(type = "text", subtype = "html", quality = 0.5),
                    MediaType(type = "Text", subtype = "html", quality = 1.0),
                    MediaType(type = "ztext", subtype = "html", quality = 0.5),
                    MediaType(type = "text", subtype = "html", quality = 1.0),
                ),
                expected = listOf(
                    // These two are considered equal
                    MediaType(type = "Text", subtype = "html", quality = 1.0),
                    MediaType(type = "text", subtype = "html", quality = 1.0),
                    MediaType(type = "text", subtype = "html", quality = 0.5),
                    MediaType(type = "ztext", subtype = "html", quality = 0.5),
                )
            ),
            TestCase(
                name = "should thirdly sort by subtype (alphabetical, case insensitive)",
                list = listOf(
                    MediaType(type = "text", subtype = "html", quality = 0.5),
                    MediaType(type = "text", subtype = "Xml", quality = 0.5),
                    MediaType(type = "text", subtype = "xml", quality = 0.5),
                    MediaType(type = "text", subtype = "html", quality = 1.0),
                ),
                expected = listOf(
                    MediaType(type = "text", subtype = "html", quality = 1.0),
                    MediaType(type = "text", subtype = "html", quality = 0.5),
                    // These two are considered equal
                    MediaType(type = "text", subtype = "xml", quality = 0.5),
                    MediaType(type = "text", subtype = "Xml", quality = 0.5),
                )
            ),
            TestCase(
                name = "should fourthly sort by parameters (key-value alphabetical, key case insensitive)",
                list = listOf(
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.5,
                        parameters = linkedMapOf("C" to "d", "x" to "z")
                    ),
                    MediaType(type = "text", subtype = "xml", quality = 0.5),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.5,
                        parameters = linkedMapOf("x" to "y", "c" to "d")
                    ),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.5,
                        parameters = linkedMapOf("x" to "y", "b" to "a")
                    ),
                ),
                expected = listOf(
                    MediaType(type = "text", subtype = "xml", quality = 0.5),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.5,
                        parameters = mapOf("b" to "a", "x" to "y")
                    ),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.5,
                        parameters = mapOf("c" to "d", "x" to "y")
                    ),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.5,
                        parameters = mapOf("c" to "d", "x" to "z")
                    ),
                )
            ),
            TestCase(
                name = "should sort by quality (nulls first), type, subtype, then parameters",
                list = listOf(
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.5,
                        parameters = linkedMapOf("c" to "d", "x" to "z")
                    ),
                    MediaType(type = "text", subtype = "html", quality = 0.5),
                    MediaType(type = "text", subtype = "xml", quality = 0.5),
                    MediaType(type = "ztext", subtype = "html", quality = 0.5),
                    MediaType(type = "text", subtype = "xml", quality = null),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.5,
                        parameters = linkedMapOf("x" to "y", "c" to "d")
                    ),
                    MediaType(type = "text", subtype = "html", quality = 1.0),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.5,
                        parameters = linkedMapOf("x" to "y", "b" to "a")
                    ),
                ),
                expected = listOf(
                    MediaType(type = "text", subtype = "xml", quality = null),
                    MediaType(type = "text", subtype = "html", quality = 1.0),
                    MediaType(type = "text", subtype = "html", quality = 0.5),
                    MediaType(type = "text", subtype = "xml", quality = 0.5),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.5,
                        parameters = mapOf("b" to "a", "x" to "y")
                    ),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.5,
                        parameters = mapOf("c" to "d", "x" to "y")
                    ),
                    MediaType(
                        type = "text",
                        subtype = "xml",
                        quality = 0.5,
                        parameters = mapOf("c" to "d", "x" to "z")
                    ),
                    MediaType(type = "ztext", subtype = "html", quality = 0.5),
                )
            )
        )
    ) {
      it.list.sortedWith(MediaType.QUALITY_VALUE_COMPARATOR) shouldBe it.expected
      // Shuffle a few times to guarantee it works in all directions
      for (ignored in 1..10) {
        it.list.shuffled()
            .sortedWith(MediaType.QUALITY_VALUE_COMPARATOR) shouldBe it.expected
      }
    }
  }
  context("of/with chains") {
    test("should work correctly") {
      MediaType.of("text", "html") shouldBe MediaType(
          type = "text",
          subtype = "html"
      )
      MediaType.of("text", "html")
          .addParameter("charset", "utf-8") shouldBe MediaType(
          type = "text",
          subtype = "html",
          parameters = mapOf(
              "charset" to "utf-8"
          )
      )
      MediaType.of("text", "html")
          .addParameter("charset", "utf-8")
          .quality(0.5) shouldBe MediaType(
          type = "text",
          subtype = "html",
          parameters = mapOf(
              "charset" to "utf-8"
          ),
          quality = 0.5
      )
    }
  }
})
