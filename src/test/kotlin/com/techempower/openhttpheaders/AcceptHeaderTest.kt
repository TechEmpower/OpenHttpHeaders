package com.techempower.openhttpheaders

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class AcceptHeaderTest : FunSpec({
  context("acceptHeader - builder") {
    context("should work for basic cases") {
      data class TestCase(
          val header: AcceptHeader,
          val expected: String
      )
      withData(
          nameFn = { it.expected },
          listOf(
              TestCase(
                  header = AcceptHeader.builder()
                      .addMediaType(
                          MediaType.builder("text", "html").build()
                      )
                      .build(),
                  expected = "text/html"
              ),
              TestCase(
                  header = AcceptHeader.builder()
                      .addMediaType(
                          MediaType.builder("application", "json")
                              .quality(0.5)
                              .build()
                      )
                      .build(),
                  expected = "application/json;q=0.5"
              ),
              TestCase(
                  header = AcceptHeader.builder()
                      .addMediaType(
                          MediaType.builder("application", "json")
                              .addParameters(
                                  "charset" to "utf-8",
                                  "extra" to "foo"
                              )
                              .quality(0.5)
                              .build()
                      )
                      .addMediaType(
                          MediaType.builder("text", "html")
                              .build()
                      )
                      .build(),
                  expected = "application/json;charset=utf-8;extra=foo;q=0.5,text/html"
              ),
              TestCase(
                  header = AcceptHeader.builder()
                      .addMediaType(
                          MediaType.builder("application", "json")
                              .addParameter("charset", "utf-8")
                              .addParameter("extra", "bar")
                              .quality(0.5)
                              .build()
                      )
                      .addMediaType(
                          MediaType.builder("text", "html")
                              .build()
                      )
                      .build(),
                  expected = "application/json;charset=utf-8;extra=bar;q=0.5,text/html"
              )
          )
      ) {
        it.header.toHeaderString() shouldBe it.expected
      }
    }
  }
  context("acceptHeader - dsl") {
    context("should work for basic cases") {
      data class TestCase(
          val header: AcceptHeader,
          val expected: String
      )
      withData(
          nameFn = { it.expected },
          listOf(
              TestCase(
                  header = acceptHeader {
                    mediaType("text", "html")
                  },
                  expected = "text/html"
              ),
              TestCase(
                  header = acceptHeader {
                    mediaType("application", "json") {
                      quality = 0.5
                    }
                  },
                  expected = "application/json;q=0.5"
              ),
              TestCase(
                  header = acceptHeader {
                    mediaType(type = "application", subtype = "json") {
                      parameters(
                          "charset" to "utf-8",
                          "extra" to "foo"
                      )
                      quality = 0.5
                    }
                    mediaType(type = "text", subtype = "html")
                  },
                  expected = "application/json;charset=utf-8;extra=foo;q=0.5,text/html"
              ),
              // Escaping/quoting
              TestCase(
                  header = acceptHeader {
                    mediaType(type = "application", subtype = "json") {
                      parameter("charset" to "utf-8")
                      parameter("extra" to "foo\" \\too")
                      quality = 0.5
                    }
                    mediaType(type = "text", subtype = "html")
                  },
                  expected = "application/json;charset=utf-8;extra=\"foo\\\" \\\\too\";q=0.5,text/html"
              )
          )
      ) {
        it.header.toHeaderString() shouldBe it.expected
      }
    }
  }
  context("parse") {
    context("should work for basic cases") {
      data class TestCase(
          val header: String,
          val expected: AcceptHeader
      )
      withData(
          nameFn = { "${it.header} -> ${it.expected}" },
          listOf(
              TestCase(
                  header = "text/html",
                  expected = AcceptHeader(
                      mediaTypes = listOf(
                          MediaType(
                              type = "text",
                              subtype = "html"
                          )
                      )
                  )
              ),
              TestCase(
                  header = "application/json;q=0.5",
                  expected = AcceptHeader(
                      mediaTypes = listOf(
                          MediaType(
                              type = "application",
                              subtype = "json",
                              quality = 0.5,
                              parameters = mapOf("q" to "0.5")
                          )
                      )
                  )
              ),
              TestCase(
                  header = "application/json;charset=utf-8;extra=foo;q=0.5,text/html",
                  expected = AcceptHeader(
                      mediaTypes = listOf(
                          MediaType(
                              type = "application",
                              subtype = "json",
                              parameters = linkedMapOf(
                                  "charset" to "utf-8",
                                  "extra" to "foo",
                                  "q" to "0.5"
                              ),
                              quality = 0.5
                          ),
                          MediaType(
                              type = "text",
                              subtype = "html"
                          )
                      )
                  )
              )
          )
      ) {
        AcceptHeader.parse(it.header) shouldBe it.expected
      }
    }
  }
  context("of/with chains") {
    test("should work correctly") {
      AcceptHeader.of(
          MediaType.of("text", "html")
      ) shouldBe AcceptHeader(
          listOf(
              MediaType.of("text", "html")
          )
      )
      AcceptHeader.of(
          MediaType.of("text", "html")
      ).addMediaType(MediaType.of("text", "xml")) shouldBe AcceptHeader(
          listOf(
              MediaType.of("text", "html"),
              MediaType.of("text", "xml")
          )
      )
    }
  }
})
