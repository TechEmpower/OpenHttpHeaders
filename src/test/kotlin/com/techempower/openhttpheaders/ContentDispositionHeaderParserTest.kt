package com.techempower.openhttpheaders

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class ContentDispositionHeaderParserTest : FunSpec({
  context("parse") {
    data class TestCase(
      val header: String,
      val expected: ContentDispositionHeader)
    withData(
      nameFn = { it.header },
      listOf(
        // Examples from MDN
        TestCase(
          header = "inline",
          expected = contentDispositionHeader(dispositionType = DispositionType.INLINE)
        ),
        TestCase(
          header = "attachment",
          expected = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT)
        ),
        TestCase(
          header = "attachment; filename=filename.jpg",
          expected = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
            filename("filename.jpg")
          }
        ),
        // Examples from spec https://www.rfc-editor.org/rfc/rfc6266#section-5
        TestCase(
          header = "attachment; filename=example.html",
          expected = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
            filename("example.html")
          }
        ),
        TestCase(
          header = "inline; filename=\"an example.html\"",
          expected = contentDispositionHeader(dispositionType = DispositionType.INLINE) {
            filename("an example.html")
          }
        ),
        TestCase(
          header = "attachment; filename*=UTF-8''%e2%82%ac%20rates",
          expected = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
            filename("€ rates", charset = Charsets.UTF_8)
          }
        ),
        TestCase(
          header = "attachment; filename=\"EURO rates\"; filename*=UTF-8''%e2%82%ac%20rates",
          expected = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
            filename("EURO rates")
            filename("€ rates", charset = Charsets.UTF_8)
          }
        ),
        // Escaping/quoting
        TestCase(
          header = "attachment; filename=\"EURO\\\"\n\\\\rates\"",
          expected = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
            filename("EURO\"\n\\rates")
          }
        ),
        // Ext-format
        TestCase(
          header = "attachment; filename*=UTF-8'en'%e2%82%ac%20rates",
          expected = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
            filename("€ rates", charset = Charsets.UTF_8, lang = "en")
          }
        ),
        TestCase(
          header = "attachment; filename*=UTF-8'en-US'%e2%82%ac%20rates",
          expected = contentDispositionHeader(dispositionType = DispositionType.ATTACHMENT) {
            filename("€ rates", charset = Charsets.UTF_8, lang = "en-US")
          }
        ),
        TestCase(
          header = "non-standard; filename*=UTF-8'en-US'%e2%82%ac%20rates",
          expected = contentDispositionHeader(dispositionType = "non-standard") {
            filename("€ rates", charset = Charsets.UTF_8, lang = "en-US")
          }
        ),
        // Nested parameter formats should not cause problems
        TestCase(
          header = "inline; abc=\";def=xyz\"; test=case",
          expected = contentDispositionHeader(dispositionType = DispositionType.INLINE) {
            parameter("abc" to ";def=xyz")
            parameter("test" to "case")
          }
        ),
      )
    ) {
      ContentDispositionHeaderParser().parse(it.header) shouldBe it.expected
    }
  }
})
