package com.techempower.openhttpheaders

import java.lang.String.CASE_INSENSITIVE_ORDER
import java.util.TreeSet

private val charactersToEscape = Regex("""(["\\])""")

private fun quoteIfNecessary(str: String): String {
  // If there are any non-token characters in the string, then it needs quotes
  if (!MediaTypeParser.FULL_TOKEN_REGEX.matches(str)) {
    val escapedStr = str.replace(charactersToEscape, "\\\\$1")
    return "\"$escapedStr\""
  }
  return str
}

/**
 * Media Types are defined per the spec, [RFC 7231 Section 3.1.1.1](https://www.rfc-editor.org/rfc/rfc7231#section-3.1.1.1).
 *
 * @throws ProcessingException if the `quality` (if provided) is less than 0,
 * greater than 1000, or has more than 3 digits after the decimal
 */
class MediaType(
    type: String,
    subtype: String,
    parameters: Map<String, String> = mapOf(),
    val quality: Double? = null
) {
  val type: String
  val subtype: String
  val parameters: Map<String, String>

  init {
    this.type = type.lowercase()
    this.subtype = subtype.lowercase()
    this.parameters =
        parameters.entries.associate { it.key.lowercase() to it.value }
    if (quality != null) {
      if (quality > 1 || quality < 0) {
        throw ProcessingException("Invalid quality, must be between 0 and 1000, inclusive: $quality")
      }
      if (quality * 1000.0 % 1.0 != 0.0) {
        throw ProcessingException("Invalid quality, may only have up to 3 numbers after the decimal place: $quality")
      }
    }
    if (type == WILDCARD && subtype != WILDCARD) {
      throw ProcessingException("Subtype may not be defined if type is wildcard: $type/$subtype")
    }
  }

  /**
   * Returns a new MediaType with the provided key-value pair added to the
   * media type's parameters.
   */
  fun addParameter(key: String, value: String) = addParameters(key to value)

  /**
   * Returns a new MediaType with the provided key-value pairs added to the
   * media type's parameters.
   */
  @JvmSynthetic
  fun addParameters(vararg entries: Pair<String, String>): MediaType {
    val newParams = parameters.toMutableMap()
    newParams.putAll(entries)
    return MediaType(
        type = type,
        subtype = subtype,
        parameters = newParams,
        quality = quality
    )
  }

  /**
   * Returns a new MediaType with the quality value for the media type set to
   * the provided value, or resets it if the provided value is `null`.
   */
  fun quality(quality: Double?) = MediaType(
      type = type,
      subtype = subtype,
      parameters = parameters,
      quality = quality
  )

  /**
   * Converts this media type into the string equivalent as defined by
   * [RFC 7231 Section 3.1.1.1](https://www.rfc-editor.org/rfc/rfc7231#section-3.1.1.1).
   *
   * The key used for the quality value may be customized by providing a value
   * for `qValueKey`. This value is `"q"` by default.
   */
  fun toMimeString(qValueKey: String = "q"): String {
    val qualityDecimal: String? = quality?.toString()
    val qualitySuffix =
        if (qualityDecimal != null) "$qValueKey=$qualityDecimal" else ""
    val parametersString = parameters
        .map { "${it.key}=${quoteIfNecessary(it.value)}" }
        .joinToString(";")
    return listOf(
        "$type/$subtype",
        parametersString,
        qualitySuffix
    )
        .filter { it.isNotBlank() }
        .joinToString(";")
  }

  /**
   * Checks to see if this media type matches the provided one according to the
   * spec for Accept header, [RFC 7231 Section 5.3.2](https://www.rfc-editor.org/rfc/rfc7231#section-5.3.2).
   * Specifically, a media type matches another if any of the following is true:
   * - either or both have the wildcard (*) type
   * - both have the same type, and either or both have the wildcard (*) subtype
   * - both have the same type and subtype
   *
   * Parameters are not considered for matching by default. If `checkParameters`
   * is `true`, then in addition to the above rules, all parameter keys present
   * in both media types must have the same values in both media types in order
   * for the media types to be considered matching. The media types may,
   * however, have keys that do not appear in the other's parameters.
   *
   * @param mediaType the other media type to check against
   * @param checkParameters if `true`, parameters will be factored in for matching
   *
   * @return `true` if both media types match
   */
  fun matches(
      mediaType: MediaType,
      checkParameters: Boolean = false
  ): Boolean {
    val typesMatch = (type.equals(mediaType.type, ignoreCase = true) &&
        (subtype.equals(mediaType.subtype, ignoreCase = true)
            || subtype == WILDCARD
            || mediaType.subtype == WILDCARD))
        || type == WILDCARD
        || mediaType.type == WILDCARD
    if (!typesMatch || !checkParameters) {
      return typesMatch
    }
    val sharedKeys = parameters.keys.intersect(mediaType.parameters.keys)
    for (sharedKey in sharedKeys) {
      if (parameters[sharedKey] != mediaType.parameters[sharedKey]) {
        return false
      }
    }
    return true
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is MediaType) return false

    if (type != other.type) return false
    if (subtype != other.subtype) return false
    if (parameters != other.parameters) return false
    if (quality != other.quality) return false

    return true
  }

  override fun hashCode(): Int {
    var result = quality?.hashCode() ?: 0
    result = 31 * result + type.hashCode()
    result = 31 * result + subtype.hashCode()
    result = 31 * result + parameters.hashCode()
    return result
  }

  /**
   * Converts this media type into the string equivalent as defined by
   * [RFC 7231 Section 3.1.1.1](https://www.rfc-editor.org/rfc/rfc7231#section-3.1.1.1).
   *
   * @see [toMimeString].
   */
  override fun toString(): String = toMimeString()

  companion object {
    /**
     * Matches anything. The wildcard value is *
     */
    private const val WILDCARD = "*"

    /**
     * Compares in the following order:
     * 1. Quality Value
     *    - Priority
     *      1. `null`
     *      2. higher values
     *      3. lower values
     * 2. Type
     *    - Case insensitive ascending, a first, z last
     * 3. Subtype
     *    - Case insensitive ascending, a first, z last
     * 4. Parameters
     *    - The entries from each parameter map are pulled by key in a
     *      case-insensitive order, then compared alphabetically ascending
     *      (a's first, z's last) case-insensitively, followed by the values,
     *      case-sensitively ascending (A's first, z's last)
     *        - Note: Parameter values are not considered case-insensitive per the spec.
     */
    val QUALITY_VALUE_COMPARATOR = Comparator<MediaType> { o1, o2 ->
      // Note: Largely based on https://github.com/spring-projects/spring-framework/blob/f93fda2a95d58dc52d14fedc11dc820017e0d8ee/spring-core/src/main/java/org/springframework/util/MimeType.java#L534-L592
      if (o1.quality == null && o2.quality != null) {
        return@Comparator -1
      }
      if (o1.quality != null && o2.quality == null) {
        return@Comparator 1
      }
      val qualityCompare = compareValues(o1.quality, o2.quality) * -1
      if (qualityCompare != 0) {
        return@Comparator qualityCompare
      }
      val typeCompare = CASE_INSENSITIVE_ORDER.compare(o1.type, o2.type)
      if (typeCompare != 0) {
        return@Comparator typeCompare
      }
      val valueCompare = CASE_INSENSITIVE_ORDER.compare(o1.subtype, o2.subtype)
      if (valueCompare != 0) {
        return@Comparator valueCompare
      }
      // TODO: Optimize
      val ownKeys = TreeSet(CASE_INSENSITIVE_ORDER)
      val o2Keys = TreeSet(CASE_INSENSITIVE_ORDER)
      ownKeys.addAll(o1.parameters.keys)
      o2Keys.addAll(o2.parameters.keys)
      val ownKeysIterator = ownKeys.iterator()
      val o2KeysIterator = o2Keys.iterator()
      for (ownKey in ownKeysIterator) {
        if (!o2KeysIterator.hasNext()) {
          return@Comparator 1
        }
        val o2Key = o2KeysIterator.next()
        val keyCompare = compareValues(ownKey, o2Key)
        if (keyCompare != 0) {
          return@Comparator keyCompare
        }
        val keyValueCompare =
            compareValues(o1.parameters[ownKey], o2.parameters[o2Key])
        if (keyValueCompare != 0) {
          return@Comparator keyValueCompare
        }
      }
      return@Comparator if (o2KeysIterator.hasNext()) -1 else 0
    }

    @JvmStatic
    fun builder(type: String, subtype: String) =
        MediaTypeBuilder(type, subtype)

    /**
     * Creates a new MediaType with the given type and subtype.
     */
    @JvmStatic
    fun of(type: String, subtype: String) = MediaType(type, subtype)

    /**
     * Parses an MediaType from the provided string based on
     * [RFC 7231 Section 3.1.1.1](https://www.rfc-editor.org/rfc/rfc7231#section-3.1.1.1).
     *
     * @throws ProcessingException if no media types are found, more than one
     * media type is found, or it fails to parse the provided string
     */
    @JvmStatic
    @JvmOverloads
    fun parse(
        mediaType: String,
        qValueKey: String = "q"
    ): MediaType {
      val mediaTypes = MediaTypeParser(qValueKey).parse(mediaType)
      if (mediaTypes.isEmpty()) {
        throw ProcessingException("No media types found")
      }
      if (mediaTypes.size > 1) {
        throw ProcessingException("More than one media type detected")
      }
      return mediaTypes[0]
    }
  }
}

class MediaTypeBuilder(
    private val type: String,
    private val subtype: String
) {
  private var parameters: MutableMap<String, String> = mutableMapOf()
  private var quality: Double? = null

  /**
   * Adds the provided key-value pair to the media type's parameters
   */
  fun addParameter(key: String, value: String): MediaTypeBuilder {
    parameters[key] = value
    return this
  }

  /**
   * Adds the provided key-value pairs to the media type's parameters
   */
  @JvmSynthetic
  fun addParameters(vararg entries: Pair<String, String>): MediaTypeBuilder {
    parameters.putAll(entries)
    return this
  }

  /**
   * Sets the quality value for the media type to the provided value, or resets
   * it if the provided value is `null`
   */
  fun quality(quality: Double?): MediaTypeBuilder {
    this.quality = quality
    return this
  }

  /**
   * @throws ProcessingException if the `quality` is less than 0, greater than
   * 1000, or has more than 3 digits after the decimal
   */
  fun build() = MediaType(
      type = type,
      subtype = subtype,
      parameters = parameters,
      quality = quality
  )
}

abstract class MediaTypeDsl(
    val type: String,
    val subtype: String
) {
  var parameters: MutableMap<String, String> = LinkedHashMap()

  /**
   * The quality value for the media type.
   */
  var quality: Double? = null

  /**
   * Adds the provided key-value pair to the media type's parameters
   */
  fun parameter(parameter: Pair<String, String>) {
    parameters[parameter.first] = parameter.second
  }

  /**
   * Adds the provided key-value pairs to the media type's parameters
   */
  fun parameters(vararg params: Pair<String, String>) {
    params.forEach { parameter(it) }
  }
}

internal class MediaTypeDslImpl(type: String, subtype: String) :
    MediaTypeDsl(type, subtype) {
  fun toMediaType() = MediaType(
      type = type,
      subtype = subtype,
      parameters = parameters.toMap(),
      quality = quality
  )
}
