package com.techempower.openhttpheaders

/**
 * The Accept header is defined by [RFC 7231 Section 5.3.2](https://www.rfc-editor.org/rfc/rfc7231#section-5.3.2).
 */
class AcceptHeader(val mediaTypes: List<MediaType>) {
  companion object {
    @JvmStatic
    fun builder() = AcceptHeaderBuilder()

    /**
     * Parses an Accept header from the provided string based on
     * [RFC 7231 Section 5.3.2](https://www.rfc-editor.org/rfc/rfc7231#section-5.3.2).
     */
    @JvmStatic
    @JvmOverloads
    fun parse(
      mediaTypes: String,
      qValueKey: String = "q"): AcceptHeader {
      return AcceptHeader(
        mediaTypes = MediaTypeParser(qValueKey).parse(
          mediaTypes
        )
      )
    }
  }

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

  override fun toString(): String {
    return "AcceptHeader(mediaTypes=$mediaTypes)"
  }
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
   */
  fun mediaType(
    type: String,
    subtype: String,
    init: MediaTypeDsl.() -> Unit = {}) {
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
