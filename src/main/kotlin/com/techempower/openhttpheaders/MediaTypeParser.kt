package com.techempower.openhttpheaders

/**
 * Parses Media Types based on the grammars defined on the following pages:
 * - [RFC 7231 Section 3.1.1.1](https://www.rfc-editor.org/rfc/rfc7231#section-3.1.1.1)
 * - [RFC 7230 Section 3.2.6](https://www.rfc-editor.org/rfc/rfc7230#section-3.2.6)
 * - [RFC 7230 Section 1.2](https://www.rfc-editor.org/rfc/rfc7230#section-1.2)
 */
internal class MediaTypeParser(private val qValueKey: String) {

  companion object {
    // Named groups
    // Note: Underscores in these names are not valid
    private const val PARAMETERS = "parameters"
    private const val TYPE = "type"
    private const val SUBTYPE = "subtype"
    private const val KEY = "key"
    private const val VALUE = "value"
    private const val QUOTED_VALUE = "quotedValue"

    private const val WILDCARD = "*"
    private val MEDIA_TYPE_REGEX: Regex
    private val PARAMETERS_REGEX: Regex
    private val Q_VALUE_REGEX: Regex = Regex("^1(\\.0{0,3})?|0(\\.\\d{0,3})?$")
    private val UNESCAPE: Regex
    val FULL_TOKEN_REGEX: Regex

    init {
      // "-" is first so that it isn't used to form a range in the regex
      val tCharRange = "-!#%&'*+.^`|~\\w$"
      val vCharRange = "!-~"
      val token = "[$tCharRange]+"
      FULL_TOKEN_REGEX = Regex("^$token$")
      val obsTextRange = "\\x80-\\xFF"
      val qdText =
          "[\t \\x21\\x23-\\x5B\\x5D-\\x7E$obsTextRange]"
      val quotedPair =
          "\\\\([\t $vCharRange$obsTextRange])"
      val quotedStr =
          "\"(?<$QUOTED_VALUE>(?:$qdText|$quotedPair)*)\""
      val ows = "[ \t]*"

      // Group 1: key
      // Group 2: unquoted value
      // Group 3: quoted value
      // The final non-capture group IS necessary, ignore the warnings to the contrary
      PARAMETERS_REGEX = Regex(
          "$ows;$ows(?<$KEY>$token)=(?:(?<$VALUE>$token)|(?:$quotedStr))"
      )

      // Group 1: type
      // Group 2: subtype
      // Group 3: parameters
      MEDIA_TYPE_REGEX = Regex(
          ",?$ows(?<$TYPE>$token)/(?<$SUBTYPE>$token)(?<$PARAMETERS>(${PARAMETERS_REGEX.pattern})*)"
      )

      UNESCAPE = Regex(quotedPair)
    }
  }

  fun parse(mediaType: String): List<MediaType> {
    // Immediately fail if a leading comma is present. The regex allows a
    // leading comma for simplicity with capturing multiple matches, but this
    // is invalid for the first match.
    if (mediaType[0] == ',') {
      throw ProcessingException(
          "Could not fully parse media type \"$mediaType\"," +
              " parsed up to position 0."
      )
    }
    val mediaTypeMatches = MEDIA_TYPE_REGEX.findAll(mediaType)
    val mediaTypes: MutableList<MediaType> = ArrayList(1)
    var mediaTypeEnd = 0
    for (mediaTypeMatchResult in mediaTypeMatches) {
      if (mediaTypeEnd != mediaTypeMatchResult.range.first) {
        throw ProcessingException(
            "Could not fully parse media type \"$mediaType\"," +
                " parsed up to position $mediaTypeEnd.",
        )
      }
      mediaTypeEnd = mediaTypeMatchResult.range.last + 1
      val type = mediaTypeMatchResult.groups[TYPE]!!.value
      val subtype = mediaTypeMatchResult.groups[SUBTYPE]!!.value
      if (type == WILDCARD && subtype != WILDCARD) {
        throw ProcessingException(
            "Invalid type/subtype combination \"$type/$subtype\" in media" +
                " type \"$mediaType\", type must be concrete if subtype is concrete."
        )
      }
      var qValue: Double? = null
      val parametersMap: MutableMap<String, String>
      if (mediaTypeMatchResult.groups[PARAMETERS] != null) {
        val parameters = mediaTypeMatchResult.groups[PARAMETERS]!!.value
        val matches = PARAMETERS_REGEX.findAll(parameters)
        parametersMap = HashMap(0)
        for (matchResult in matches) {
          val key = matchResult.groups[KEY]!!.value
          val value: String = if (matchResult.groups[VALUE] != null) {
            matchResult.groups[VALUE]!!.value
          } else {
            val quotedValue = matchResult.groups[QUOTED_VALUE]!!.value
            UNESCAPE.replace(quotedValue, "$1")
          }
          parametersMap[key] = value
          if (key.equals(qValueKey, ignoreCase = true)) {
            qValue = try {
              if (!Q_VALUE_REGEX.matches(value)) {
                throw ProcessingException(
                    "Invalid q-value format \"$value\" in media type \"$mediaType\". The " +
                        "q-value must be between 0 and 1 with at most 3 decimal places.",
                )
              }
              value.toDouble()
            } catch (e: NumberFormatException) {
              throw ProcessingException(
                  "Invalid q-value \"$value\" in media type \"$mediaType\"," +
                      " failed to parse number.", e
              )
            }
            break
          }
        }
      } else {
        parametersMap = mutableMapOf()
      }
      mediaTypes.add(
          MediaType(
              type,
              subtype,
              parametersMap,
              qValue
          )
      )
    }
    if (mediaTypeEnd != mediaType.length) {
      throw ProcessingException(
          "Could not fully parse media type \"$mediaType\"," +
              " parsed up to position $mediaTypeEnd.",
      )
    }
    return mediaTypes.toList()
  }
}
