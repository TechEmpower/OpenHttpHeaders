package com.techempower.openhttpheaders

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import java.nio.charset.Charset

// TODO: https://www.baeldung.com/kotlin/kotest#10-test-coverage

class ContentDispositionHeaderTest : FunSpec({
  context("contentDispositionHeader - dsl") {
    data class TestCase(
        val header: ContentDispositionHeader,
        val expected: String
    )
    context("should work for basic cases") {
      withData(
          nameFn = { it.expected },
          listOf(
              // Examples from MDN https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
              TestCase(
                  header = contentDispositionHeader(dispositionType = DispositionType.INLINE),
                  expected = "inline"
              ),
              TestCase(
                  header = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT),
                  expected = "attachment"
              ),
              TestCase(
                  header = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
                    filename("filename.jpg")
                  },
                  expected = "attachment; filename=filename.jpg"
              ),
              // Examples from spec https://www.rfc-editor.org/rfc/rfc6266#section-5
              TestCase(
                  header = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
                    filename("example.html")
                  },
                  expected = "attachment; filename=example.html"
              ),
              TestCase(
                  header = contentDispositionHeader(dispositionType = DispositionType.INLINE) {
                    filename("an example.html")
                  },
                  expected = "inline; filename=\"an example.html\""
              ),
              TestCase(
                  header = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
                    filename("€ rates", charset = Charsets.UTF_8)
                  },
                  expected = "attachment; filename*=UTF-8''%e2%82%ac%20rates"
              ),
              TestCase(
                  header = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
                    filename("EURO rates")
                    filename("€ rates", charset = Charsets.UTF_8)
                  },
                  expected = "attachment; filename=\"EURO rates\"; filename*=UTF-8''%e2%82%ac%20rates"
              ),
              // Escaping/quoting
              TestCase(
                  header = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
                    filename("EURO\"\n\\rates")
                  },
                  expected = "attachment; filename=\"EURO\\\"\n\\\\rates\""
              ),
              // Ext-format
              TestCase(
                  header = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
                    filename("€ rates", charset = Charsets.UTF_8, lang = "en")
                  },
                  expected = "attachment; filename*=UTF-8'en'%e2%82%ac%20rates"
              ),
              TestCase(
                  header = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
                    filename(
                        "€ rates",
                        charset = Charsets.UTF_8,
                        lang = "en-US"
                    )
                  },
                  expected = "attachment; filename*=UTF-8'en-US'%e2%82%ac%20rates"
              ),
              TestCase(
                  header = contentDispositionHeader(dispositionType = "non-standard") {
                    filename(
                        "€ rates",
                        charset = Charsets.UTF_8,
                        lang = "en-US"
                    )
                  },
                  expected = "non-standard; filename*=UTF-8'en-US'%e2%82%ac%20rates"
              ),
          )
      ) {
        it.header.toHeaderString() shouldBeEqualIgnoringCase it.expected
      }
    }
  }
  context("contentDispositionHeader - builder") {
    data class TestCase(
        val header: ContentDispositionHeader,
        val expected: String
    )
    withData(
        nameFn = { it.expected },
        listOf(
            // Examples from MDN
            TestCase(
                header = ContentDispositionHeader.builder(dispositionType = DispositionType.INLINE)
                    .build(),
                expected = "inline"
            ),
            TestCase(
                header = ContentDispositionHeader.builder(dispositionType = DispositionType.ATTACHMENT)
                    .build(),
                expected = "attachment"
            ),
            TestCase(
                header = ContentDispositionHeader.builder(dispositionType = DispositionType.ATTACHMENT)
                    .filename("filename.jpg")
                    .build(),
                expected = "attachment; filename=filename.jpg"
            ),
            // Examples from spec https://www.rfc-editor.org/rfc/rfc6266#section-5
            TestCase(
                header = ContentDispositionHeader.builder(dispositionType = DispositionType.ATTACHMENT)
                    .filename("example.html")
                    .build(),
                expected = "attachment; filename=example.html"
            ),
            TestCase(
                header = ContentDispositionHeader.builder(dispositionType = DispositionType.INLINE)
                    .filename("an example.html")
                    .build(),
                expected = "inline; filename=\"an example.html\""
            ),
            TestCase(
                header = ContentDispositionHeader.builder(dispositionType = DispositionType.ATTACHMENT)
                    .filename("€ rates", charset = Charsets.UTF_8)
                    .build(),
                expected = "attachment; filename*=UTF-8''%e2%82%ac%20rates"
            ),
            TestCase(
                header = ContentDispositionHeader.builder(dispositionType = DispositionType.ATTACHMENT)
                    .filename("EURO rates")
                    .filename("€ rates", charset = Charsets.UTF_8)
                    .build(),
                expected = "attachment; filename=\"EURO rates\"; filename*=UTF-8''%e2%82%ac%20rates"
            ),
            // Escaping/quoting
            TestCase(
                header = ContentDispositionHeader.builder(dispositionType = DispositionType.ATTACHMENT)
                    .filename("EURO\"\n\\rates")
                    .build(),
                expected = "attachment; filename=\"EURO\\\"\n\\\\rates\""
            ),
            // Ext-format
            TestCase(
                header = ContentDispositionHeader.builder(dispositionType = DispositionType.ATTACHMENT)
                    .filename("€ rates", charset = Charsets.UTF_8, lang = "en")
                    .build(),
                expected = "attachment; filename*=UTF-8'en'%e2%82%ac%20rates"
            ),
            TestCase(
                header = ContentDispositionHeader.builder(dispositionType = DispositionType.ATTACHMENT)
                    .filename(
                        "€ rates",
                        charset = Charsets.UTF_8,
                        lang = "en-US"
                    )
                    .build(),
                expected = "attachment; filename*=UTF-8'en-US'%e2%82%ac%20rates"
            ),
            TestCase(
                header = ContentDispositionHeader.builder(dispositionType = "non-standard")
                    .filename(
                        "€ rates",
                        charset = Charsets.UTF_8,
                        lang = "en-US"
                    )
                    .build(),
                expected = "non-standard; filename*=UTF-8'en-US'%e2%82%ac%20rates"
            ),
            TestCase(
                header = ContentDispositionHeader.builder(dispositionType = "non-standard")
                    .filename(
                        "€ rates",
                        charset = Charsets.UTF_8,
                        lang = "en-US"
                    )
                    .addParameter(key = "test", value = "case")
                    .build(),
                expected = "non-standard; filename*=UTF-8'en-US'%e2%82%ac%20rates; test=case"
            ),
        )
    ) {
      it.header.toHeaderString() shouldBeEqualIgnoringCase it.expected
    }
  }
  context("filename should be stored in lowercase") {
    val filenameInputKey = "FILENAME"
    val filenameKey = "filename"
    val exampleFilename = "test.txt"

    class TestCase(
        val name: String,
        val header: ContentDispositionHeader
    )

    withData(
        nameFn = { it.name },
        listOf(
            TestCase(
                name = "builder + filename method",
                header = ContentDispositionHeader.builder(DispositionType.ATTACHMENT)
                    .filename(exampleFilename)
                    .build()
            ),
            TestCase(
                name = "builder + addParameter standard method",
                header = ContentDispositionHeader.builder(DispositionType.ATTACHMENT)
                    .addParameter(filenameInputKey, exampleFilename)
                    .build()
            ),
            TestCase(
                name = "builder + addParameter Pair method",
                header = ContentDispositionHeader.builder(DispositionType.ATTACHMENT)
                    .addParameter(filenameInputKey to exampleFilename)
                    .build()
            ),
            TestCase(
                name = "DSL + filename method",
                header = contentDispositionHeader(DispositionType.ATTACHMENT) {
                  filename(exampleFilename)
                }
            ),
            TestCase(
                name = "DSL + addParameter method",
                header = contentDispositionHeader(DispositionType.ATTACHMENT) {
                  parameter(filenameInputKey to exampleFilename)
                }
            )
        )
    ) {
      it.header.getParameter(filenameKey)?.value shouldBe exampleFilename
      it.header.getParameter(filenameInputKey)?.value shouldBe exampleFilename
      it.header.getParameters(filenameInputKey)
          .map { parameter -> parameter.value } shouldBe listOf(exampleFilename)
    }
  }
  context("getParameter*") {
    context("non-ext values should be prioritized over ext-values") {
      val standardFilename = "EURO rates"
      val extFilename = "€ rates"

      class TestCase(
          val name: String,
          val header: ContentDispositionHeader
      )
      withData(
          nameFn = { it.name },
          listOf(
              TestCase(
                  name = "charset - order 1",
                  header = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
                    filename(standardFilename)
                    filename(extFilename, charset = Charsets.UTF_8)
                  }
              ),
              TestCase(
                  name = "charset - order 2",
                  header = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
                    filename(extFilename, charset = Charsets.UTF_8)
                    filename(standardFilename)
                  }
              ),
              TestCase(
                  name = "lang - order 1",
                  header = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
                    filename(standardFilename)
                    filename(extFilename, lang = "en")
                  }
              ),
              TestCase(
                  name = "lang - order 2",
                  header = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
                    filename(extFilename, lang = "en")
                    filename(standardFilename)
                  }
              ),
              // Generally this library only considers ext to be when either lang
              // is defined or the charset is anything other than ISO-8859-1, but
              // it's technically possible to use the ext format while using
              // ISO-8859-1 and no lang. For this reason, these tests exist to
              // verify that the parser still captures this for the purpose of
              // sorting, even if it isn't shown to the end user.
              TestCase(
                  name = "parsed unnecessary ext - order 1",
                  header = ContentDispositionHeader.parse("attachment; filename=\"$standardFilename\"; filename*=ISO-8859-1''example%20value")
              ),
              TestCase(
                  name = "parsed unnecessary ext - order 2",
                  header = ContentDispositionHeader.parse("attachment; filename*=ISO-8859-1''example%20value; filename=\"$standardFilename\"")
              ),
          )
      ) {
        it.header.getFilename() shouldBe standardFilename
      }
    }
  }
  context("of/with chains") {
    test("should work correctly") {
      ContentDispositionHeader.of(DispositionType.ATTACHMENT) shouldBe ContentDispositionHeader(
          dispositionType = "attachment"
      )
      ContentDispositionHeader.of("test") shouldBe ContentDispositionHeader(
          dispositionType = "test"
      )
      ContentDispositionHeader.of(DispositionType.ATTACHMENT)
          .addParameter("test" to "value", Charsets.UTF_8, lang = "en") shouldBe ContentDispositionHeader(
          dispositionType = "attachment",
          parameters = listOf(
              ContentDispositionHeader.Parameter(
                  key = "test",
                  value = "value",
                  charset = Charsets.UTF_8,
                  lang = "en",
                  explicitExt = false
              )
          )
      )
      ContentDispositionHeader.of(DispositionType.ATTACHMENT)
          .addParameter("test", "value", Charsets.UTF_8, lang = "en") shouldBe ContentDispositionHeader(
          dispositionType = "attachment",
          parameters = listOf(
              ContentDispositionHeader.Parameter(
                  key = "test",
                  value = "value",
                  charset = Charsets.UTF_8,
                  lang = "en",
                  explicitExt = false
              )
          )
      )
      ContentDispositionHeader.of(DispositionType.ATTACHMENT)
          .filename("file.txt", Charsets.UTF_8, lang = "en") shouldBe ContentDispositionHeader(
          dispositionType = "attachment",
          parameters = listOf(
              ContentDispositionHeader.Parameter(
                  key = "filename",
                  value = "file.txt",
                  charset = Charsets.UTF_8,
                  lang = "en",
                  explicitExt = false
              )
          )
      )
    }
  }
})
