package com.techempower.openhttpheaders

/**
 * The Accept header is defined by [RFC 7231 Section 5.3.2](https://www.rfc-editor.org/rfc/rfc7231#section-5.3.2).
 */
class AcceptHeader(val mediaTypes: List<MediaType>) {
  companion object {
    @JvmStatic
    fun builder() = AcceptHeaderBuilder()

    /**
     * Creates a new AcceptHeader with the given media types.
     */
    @JvmStatic
    fun of(mediaType: MediaType, vararg mediaTypes: MediaType) = AcceptHeader(mediaTypes = (listOf(mediaType) + mediaTypes).toList())

    /**
     * Creates a new AcceptHeader with the given media types.
     */
    @JvmStatic
    fun of(mediaTypes: List<MediaType>) = AcceptHeader(mediaTypes = mediaTypes)

    /**
     * Parses an Accept header from the provided string based on
     * [RFC 7231 Section 5.3.2](https://www.rfc-editor.org/rfc/rfc7231#section-5.3.2).
     *
     * @throws ProcessingException if it fails to parse the provided string
     */
    @JvmStatic
    @JvmOverloads
    fun parse(
        mediaTypes: String,
        qValueKey: String = "q"
    ): AcceptHeader {
      return AcceptHeader(
          mediaTypes = MediaTypeParser(qValueKey).parse(
              mediaTypes
          )
      )
    }
  }

  /**
   * Returns a new Accept header with the provided media type added.
   */
  fun addMediaType(mediaType: MediaType) = AcceptHeader(mediaTypes + mediaType)

  /**
   * Converts this Accept header into the string equivalent as defined by
   * [RFC 7231 Section 5.3.2](https://www.rfc-editor.org/rfc/rfc7231#section-5.3.2),
   * and by extension, [RFC 7231 Section 3.1.1.1](https://www.rfc-editor.org/rfc/rfc7231#section-3.1.1.1).
   */
  @JvmOverloads
  fun toHeaderString(qValueKey: String = "q") =
      mediaTypes.joinToString(",") { it.toMimeString(qValueKey) }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AcceptHeader) return false

    if (mediaTypes != other.mediaTypes) return false

    return true
  }

  override fun hashCode(): Int {
    return mediaTypes.hashCode()
  }

  /**
   * Converts this Accept header into the string equivalent as defined by
   * [RFC 7231 Section 5.3.2](https://www.rfc-editor.org/rfc/rfc7231#section-5.3.2),
   * and by extension, [RFC 7231 Section 3.1.1.1](https://www.rfc-editor.org/rfc/rfc7231#section-3.1.1.1).
   */
  override fun toString(): String = toHeaderString()
}

@JvmSynthetic
fun acceptHeader(init: AcceptHeaderDsl.() -> Unit) =
    AcceptHeaderDslImpl()
        .also(init)
        .toHeader()


class AcceptHeaderBuilder {
  private val types: MutableList<MediaType> = mutableListOf()

  /**
   * Adds the provided media type to the media types for the header.
   */
  fun addMediaType(type: MediaType): AcceptHeaderBuilder {
    types.add(type)
    return this
  }

  fun build() = AcceptHeader(mediaTypes = types)
}

abstract class AcceptHeaderDsl {
  /**
   * Adds the provided media type to the Accept header
   */
  abstract fun mediaType(mediaType: MediaType)

  /**
   * Adds the provided media type to the Accept header
   *
   *
   * @throws ProcessingException if the `quality` (if provided) is less than 0,
   * greater than 1000, or has more than 3 digits after the decimal
   */
  fun mediaType(
      type: String,
      subtype: String,
      init: MediaTypeDsl.() -> Unit = {}
  ) {
    mediaType(MediaTypeDslImpl(type, subtype).also(init).toMediaType())
  }
}

// By having this class separate from the above, and private, it allows for
// internal-only functions to stay hidden to the end user, such as `toHeader`.
private class AcceptHeaderDslImpl : AcceptHeaderDsl() {
  var mediaTypes: MutableList<MediaType> = mutableListOf()

  override fun mediaType(mediaType: MediaType) {
    mediaTypes.add(mediaType)
  }

  fun toHeader(): AcceptHeader {
    return AcceptHeader(mediaTypes = mediaTypes)
  }
}
