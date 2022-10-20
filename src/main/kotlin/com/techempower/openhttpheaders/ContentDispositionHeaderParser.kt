package com.techempower.openhttpheaders

import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Parses Content Disposition based on the grammars defined on the following pages:
 * - [RFC 6266 Section 4.1](https://www.rfc-editor.org/rfc/rfc6266#section-4.1)
 * - [RFC 2616 Section 2.2](https://www.rfc-editor.org/rfc/rfc2616#section-2.2)
 * - [RFC 2616 Section 3.6](https://www.rfc-editor.org/rfc/rfc2616#section-3.6)
 * - [RFC 5987 Section 3.2](https://www.rfc-editor.org/rfc/rfc5987#section-3.2)
 * - [RFC 5987 Section 2](https://www.rfc-editor.org/rfc/rfc5987#section-2).
 * - [RFC 5646 Section 2.1](https://www.rfc-editor.org/rfc/rfc5646#section-2.1)
 * - [RFC 5234 Appendix B.1](https://www.rfc-editor.org/rfc/rfc5234#appendix-B.1)
 * - [RFC 2616 Section 2.1: implied *LWS](https://www.rfc-editor.org/rfc/rfc2616#section-2.1)
 */
internal class ContentDispositionHeaderParser {
  companion object {

    // Named groups
    // Note: Underscores in these names are not valid
    private const val TYPE = "type"
    private const val QuotedValue = "quotedValue"
    private const val KEY = "key"
    private const val VALUE = "value"
    private const val CHARSET = "charset"
    private const val LANGUAGE = "language"
    private const val ExtValueChars = "extValueChars"
    private const val ExtKey = "extKey"

    private val TYPE_REGEX: Regex
    private val DISPOSITION_PARAM_REGEX: Regex
    private val UNESCAPE: Regex
    val FULL_TOKEN_REGEX: Regex

    init {
      val ctlRange = "\\000-\\037\\0127"
      val separatorsRange = """()<>@,;:\\"/\[\]?={} \t"""
      val charRange = """\x00-\x7F"""
      // Note: This could be optimized at a later time to where it's inlined to
      // just be a single range with all the excluded characters removed. But
      // for now this simply better reflects the grammar 1:1
      val token = """(?:(?![$ctlRange$separatorsRange])[$charRange])+"""
      FULL_TOKEN_REGEX = Regex("^$token$")
      val lws = "(?:(\r\n)?[ \t]+)?"

      @Suppress("UnnecessaryVariable") // While this is an unnecessary
      // variable, it's referred to as optional whitespace in the grammar
      // notes, which itself is defined as "linear whitespace" elsewhere. So to
      // keep consistent with the grammar most closely tied with
      // Content Disposition itself, this is to be referred to as `ows` in this
      // regex, not `lws`
      val ows = lws

      // grammar for `text`: <any OCTET except CTLs, but including LWS>
      // This has been expressed as
      // {all ascii characters after the CTLs, plus LWS}: [\x20-\x7E\r\n\t]
      // or {any non ascii character}: [^\x00-xFF]
      // language=regexp
      val text = """([\x20-\x7E\r\n\t]|[^\x00-\xFF])"""
      val qdText = """(?:(?!")$text)"""
      // language=regexp
      val char = "[\\x00-\\x7F]"
      val quotedPair = """\\($char)"""
      // Note: quotedPair MUST be captured before qdText, as it is technically
      // a valid and capture-able qdText value, but SHOULD be interpreted as a
      // quoted pair.
      // language=regexp
      val quotedString = """(?:"(?<$QuotedValue>(?:$quotedPair|$qdText)*)")"""
      // language=regexp
      val tokenThenValue =
          "(?:(?<$KEY>$token)$ows=$ows(?:(?<$VALUE>$token)|$quotedString))"
      // language=regexp
      val mimeCharsetCharacterRange = "[-a-zA-Z0-9!#$%&+^_`{}~]"

      // Language Tag definition >>>

      // language=regexp
      val alpha = "[a-zA-Z]"
      // language=regexp
      val digit = "[0-9]"
      // language=regexp
      val alphanum = "[a-zA-Z0-9]"
      // Reserves 'x'/'X'
      // language=regexp
      val singleton = "[0-9a-wyzA-WYZ]"

      // language=regexp
      val privateUse = "(?:x(?:-$alphanum{1,8}))"
      // language=regexp
      val extLang = "(?:$alpha{3}(?:-$alpha{3}){0,2})"
      // Seems like the last part of the below could just be $alpha{4,8}, but
      // his is how it's defined in the grammar, likely to describe more
      // general concepts
      // language=regexp
      val language = "(?:$alpha{2,3}(?:-$extLang)?|$alpha{4}|$alpha{5,8})"
      // language=regexp
      val script = "(?:$alpha{4})"
      // language=regexp
      val region = "(?:$alpha{2}|$digit{3})"
      // language=regexp
      val variant = "(?:$alphanum{5,8}|$digit$alphanum{3})"
      // language=regexp
      val extension = "(?:$singleton(?:-$alphanum{2,8})+)"
      // language=regexp
      val langTag =
          "(?:$language(?:-$script)?(?:-$region)?(?:-$variant)*(?:-$extension)*(?:-$privateUse)?)"

      // language=regexp
      val irregular = "(?:${
        listOf(
            "en-GB-oed",
            "i-ami",
            "i-bnn",
            "i-default",
            "i-enochian",
            "i-hak",
            "i-klingon",
            "i-lux",
            "i-mingo",
            "i-navajo",
            "i-pwn",
            "i-tao",
            "i-tay",
            "i-tsu",
            "sgn-BE-FR",
            "sgn-BE-NL",
            "sgn-CH-DE",
        ).joinToString("|")
      })"
      // language=regexp
      val regular = "(?:${
        listOf(
            "art-lojban",
            "cel-gaulish",
            "no-bok",
            "no-nyn",
            "zh-guoyu",
            "zh-hakka",
            "zh-min",
            "zh-min-nan",
            "zh-xiang",
        ).joinToString("|")
      })"
      // language=regexp
      val grandfathered = "(?:$irregular|$regular)"

      val languageTag = "(?:$langTag|$privateUse|$grandfathered)"
      // <<< Language Tag definition

      // language=regexp
      val percentEncoded = "(?:%[0-9a-fA-F]{2})"
      // language=regexp
      val attrChar = "[-a-zA-Z0-9!#$&+.^_`|~]"

      // Note that ' is not defined to be a separator, so ows is not allowed surrounding it
      // language=regexp
      val extValue =
          "(?<$CHARSET>$mimeCharsetCharacterRange+)'(?<$LANGUAGE>$languageTag)?'(?<$ExtValueChars>(?:$percentEncoded|$attrChar)*)"
      // language=regexp
      val extTokenThenExtValue =
          """(?:(?:(?<$ExtKey>$token)\*)$ows=$ows(?:$extValue))"""

      TYPE_REGEX = Regex("^(?<$TYPE>$token)")
      // Note: extTokenThenExtValue MUST be captured first otherwise it will
      // always be captured as a token/value, as the token/value grammar
      // completely contains the grammar/characters present in an ext value.
      // Also, per the grammar of token, "key*" is a valid token, meaning an
      // incorrectly formatted ext pair can and will instead be interpreted as
      // a token-value pair. While it's likely that the intention was to always
      // interpret "key*" keys as an ext-token, the spec does not prevent it
      // from being a standard key-value token, and for that reason this
      // implementation does not make any assumptions.
      DISPOSITION_PARAM_REGEX =
          Regex("$ows;$ows(?:$extTokenThenExtValue|$tokenThenValue)")
      UNESCAPE = Regex(quotedPair)
    }
  }

  fun parse(headerStr: String): ContentDispositionHeader {
    val typeMatchResult = TYPE_REGEX.find(headerStr)
        ?: throw ProcessingException(
            "Could not fully parse content disposition \"$headerStr\"," +
                " parsed up to position 0."
        )
    val builder =
        ContentDispositionHeader.builder(typeMatchResult.groups[TYPE]!!.value)
    var parseIndex = typeMatchResult.range.last + 1
    for (dispositionParamMatchResult in DISPOSITION_PARAM_REGEX.findAll(
        headerStr, startIndex = parseIndex
    )) {
      val key = dispositionParamMatchResult.groups[KEY]?.value
      val value = dispositionParamMatchResult.groups[VALUE]?.value
      val quotedValue = dispositionParamMatchResult.groups[QuotedValue]?.value
      val extKey = dispositionParamMatchResult.groups[ExtKey]?.value
      val charsetStr = dispositionParamMatchResult.groups[CHARSET]?.value
      val language = dispositionParamMatchResult.groups[LANGUAGE]?.value
      val extValueChars =
          dispositionParamMatchResult.groups[ExtValueChars]?.value
      if (key != null) {
        val providedValue = value ?: UNESCAPE.replace(quotedValue!!, "$1")
        builder.addParameter(key to providedValue)
      } else if (extKey != null) {
        val charset = Charset.forName(charsetStr)
        val extValue = URLDecoder.decode(extValueChars, charsetStr)
        builder.addParameter(
            extKey to extValue,
            charset = charset,
            lang = language,
            explicitExt = true
        )
      } else {
        throw ProcessingException(
            "Could not fully parse content disposition \"$headerStr\"," +
                " parsed up to position ${dispositionParamMatchResult.range.first}."
        )
      }
      parseIndex = dispositionParamMatchResult.range.last + 1
    }
    if (parseIndex != headerStr.length) {
      throw ProcessingException(
          "Could not fully parse content disposition \"$headerStr\"," +
              " parsed up to position $parseIndex (${
                headerStr.substring(
                    parseIndex
                )
              })"
      )
    }
    return builder.build()
  }
}

