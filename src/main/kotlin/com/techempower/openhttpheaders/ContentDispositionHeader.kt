package com.techempower.openhttpheaders

import java.net.URLEncoder
import java.nio.charset.Charset

// TODO: Content-Type will also have a Content-Disposition header, but
//  according to the spec this relies on, this class should not be re-used for
//  that. Look into that further.

private val charactersToEscape = Regex("""(["\\])""")
private const val FILENAME_KEY = "filename"

private fun lowercaseIfFilename(key: String) =
    if (key.equals(FILENAME_KEY, ignoreCase = true)) FILENAME_KEY else key

private fun quoteIfNecessary(str: String): String {
  if (!ContentDispositionHeaderParser.FULL_TOKEN_REGEX.matches(str)) {
    val escapedStr = str.replace(charactersToEscape, "\\\\$1")
    return "\"$escapedStr\""
  }
  return str
}

/**
 * Content Disposition is defined by [RFC 6266](https://www.rfc-editor.org/rfc/rfc6266).
 * The disposition type is case-insensitive, and `filename` in the parameters
 * is case-insensitive, but all other parameters are case-sensitive.
 */
class ContentDispositionHeader(
    dispositionType: String,
    val parameters: List<Parameter> = listOf()
) {

  val dispositionType: String
  private val sortedParameters: List<Parameter>

  init {
    this.dispositionType = dispositionType.lowercase()
    // Per the spec, non-ext parameter values should be prioritized over ext
    // parameters when both exist for the same key.
    sortedParameters = parameters.sortedBy { it.isExt() }
  }

  /**
   * Gets the first parameter with the given key, if present. Prioritizes
   * non-ext parameter keys.
   */
  fun getParameter(key: String): Parameter? {
    val normalizedKey = lowercaseIfFilename(key)
    return sortedParameters.find { it.key == normalizedKey }
  }

  /**
   * Gets all parameters with the given key. Parameters with non-ext parameter
   * keys are sorted to the front.
   */
  fun getParameters(key: String): List<Parameter> {
    val normalizedKey = lowercaseIfFilename(key)
    return sortedParameters.filter { it.key == normalizedKey }
  }

  fun getFilename(): String? = getParameter(FILENAME_KEY)?.value

  /**
   * Returns a new ContentDispositionHeader with the provided filename-value
   * pair added to the Content Disposition's parameters. If `charset` is
   * anything other than [Charsets.ISO_8859_1], the value is encoded in the ext
   * format, and provided as `filename*=ext-value`. Lang may optionally be
   * provided, and if present will also cause the use of the ext format.
   */
  @JvmOverloads
  fun filename(
      value: String,
      charset: Charset = Charsets.ISO_8859_1,
      lang: String? = null
  ): ContentDispositionHeader {
    return addParameter(FILENAME_KEY to value, charset, lang)
  }

  /**
   * Returns a new ContentDispositionHeader with the provided key-value
   * pair added to the Content Disposition's parameters. If `charset` is
   * anything other than [Charsets.ISO_8859_1], the value is encoded in the ext
   * format, and provided as `filename*=ext-value`. Lang may optionally be
   * provided, and if present will also cause the use of the ext format.
   */
  @JvmOverloads
  fun addParameter(
      key: String,
      value: String,
      charset: Charset = Charsets.ISO_8859_1,
      lang: String? = null
  ): ContentDispositionHeader {
    return addParameter(key to value, charset, lang)
  }

  /**
   * Returns a new ContentDispositionHeader with the provided key-value
   * pair added to the Content Disposition's parameters. If `charset` is
   * anything other than [Charsets.ISO_8859_1], the value is encoded in the ext
   * format, and provided as `filename*=ext-value`. Lang may optionally be
   * provided, and if present will also cause the use of the ext format.
   */
  @JvmSynthetic
  fun addParameter(
      parameter: Pair<String, String>,
      charset: Charset = Charsets.ISO_8859_1,
      lang: String? = null
  ): ContentDispositionHeader {
    val newParams = parameters.toMutableList()
    newParams.add(Parameter(parameter.first, parameter.second, charset, lang, false))
    return ContentDispositionHeader(
        dispositionType = dispositionType,
        parameters = newParams
    )
  }

  /**
   * Converts this header into the string equivalent as defined by
   * [RFC 6266 Section 4.1](https://www.rfc-editor.org/rfc/rfc6266#section-4.1).
   */
  fun toHeaderString(): String {
    val parametersSuffix = if (parameters.isEmpty()) "" else {
      "; " + parameters.joinToString("; ") { it.toParameterString() }
    }
    return "$dispositionType$parametersSuffix"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ContentDispositionHeader) return false

    if (parameters != other.parameters) return false
    if (dispositionType != other.dispositionType) return false

    return true
  }

  override fun hashCode(): Int {
    var result = parameters.hashCode()
    result = 31 * result + dispositionType.hashCode()
    return result
  }

  /**
   * Converts this header into the string equivalent as defined by
   * [RFC 6266 Section 4.1](https://www.rfc-editor.org/rfc/rfc6266#section-4.1).
   */
  override fun toString(): String = toHeaderString()

  companion object {

    @JvmStatic
    fun builder(dispositionType: DispositionType): ContentDispositionHeaderBuilder {
      return ContentDispositionHeaderBuilder(dispositionType)
    }

    @JvmStatic
    fun builder(dispositionType: String): ContentDispositionHeaderBuilder {
      return ContentDispositionHeaderBuilder(dispositionType)
    }

    /**
     * Creates a new ContentDispositionHeader with the given disposition type.
     */
    @JvmStatic
    fun of(dispositionType: String) = ContentDispositionHeader(dispositionType)

    /**
     * Creates a new ContentDispositionHeader with the given disposition type.
     */
    @JvmStatic
    fun of(dispositionType: DispositionType) = ContentDispositionHeader(dispositionType.value)

    /**
     * Parses a Content Disposition header from the provided string based on
     * [RFC 6266 Section 4.1](https://www.rfc-editor.org/rfc/rfc6266#section-4.1).
     *
     * @throws ProcessingException if it fails to parse the provided string
     */
    @JvmStatic
    fun parse(str: String) = ContentDispositionHeaderParser().parse(str)
  }

  /**
   * A single parameter within a Content Disposition header. Effectively
   * corresponds to the `disp-ext-parm` token within the grammar defined in
   * [RFC 6266 Section 4.1](https://www.rfc-editor.org/rfc/rfc6266#section-4.1).
   */
  class Parameter internal constructor(
      val key: String,
      val value: String,
      val charset: Charset,
      val lang: String?,
      private val explicitExt: Boolean
  ) {

    internal fun toParameterString(): String {
      val key: String
      val value: String
      if (isExt()) {
        key = "${this.key}*"
        val encoded = URLEncoder.encode(this.value, charset)
            .replace("+", "%20")
        value = "${charset.name()}'${lang ?: ""}'$encoded"
      } else {
        key = this.key
        value = quoteIfNecessary(this.value)
      }
      return "$key=$value"
    }

    fun isExt(): Boolean =
        explicitExt || charset != Charsets.ISO_8859_1 || !lang.isNullOrEmpty()

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Parameter) return false

      if (key != other.key) return false
      if (value != other.value) return false
      if (charset != other.charset) return false
      if (lang != other.lang) return false
      // Note: explicitExt is not included as it's not really relevant to the
      // parameter itself, nor can it be updated by the end user
      //if (explicitExt != other.explicitExt) return false

      return true
    }

    override fun hashCode(): Int {
      var result = key.hashCode()
      result = 31 * result + value.hashCode()
      result = 31 * result + charset.hashCode()
      result = 31 * result + (lang?.hashCode() ?: 0)
      // Note: explicitExt is not included as it's not really relevant to the
      // parameter itself, nor can it be updated by the end user
      //result = 31 * result + explicitExt.hashCode()
      return result
    }

    override fun toString(): String {
      return "Parameter(key='$key', value='$value', charset=$charset, lang=$lang)"
    }
  }
}

@JvmSynthetic
fun contentDispositionHeader(
    dispositionType: String,
    init: ContentDispositionHeaderDsl.() -> Unit = {}
) =
    ContentDispositionHeaderDslImpl(dispositionType)
        .also(init)
        .toHeader()

@JvmSynthetic
fun contentDispositionHeader(
    dispositionType: DispositionType,
    init: ContentDispositionHeaderDsl.() -> Unit = {}
) =
    ContentDispositionHeaderDslImpl(dispositionType)
        .also(init)
        .toHeader()

/**
 * The meaningful disposition types defined by the `disposition-type` token in
 * [RFC 6266 Section 4.1](https://www.rfc-editor.org/rfc/rfc6266#section-4.1).
 */
enum class DispositionType(val value: String) {
  INLINE("inline"),
  ATTACHMENT("attachment")
}

class ContentDispositionHeaderBuilder(private val dispositionType: String) {
  private val parameters: MutableList<ContentDispositionHeader.Parameter> =
      mutableListOf()

  constructor(dispositionType: DispositionType) : this(dispositionType.value)

  /**
   * Adds the provided filename-value pair to the Content Disposition's
   * parameters. If `charset` is anything other than [Charsets.ISO_8859_1],
   * the value is encoded in the ext format, and provided as `filename*=ext-value`.
   * Lang may optionally be provided, and if present will also cause the use of
   * the ext format.
   */
  @JvmOverloads
  fun filename(
      value: String,
      charset: Charset = Charsets.ISO_8859_1,
      lang: String? = null
  ): ContentDispositionHeaderBuilder {
    addParameter(FILENAME_KEY to value, charset, lang)
    return this
  }

  /**
   * Adds the provided key-value pair to the Content Disposition's parameters.
   * If `charset` is anything other than [Charsets.ISO_8859_1], the
   * value is encoded in the ext format, and provided as `key*=ext-value`.
   * Lang may optionally be provided, and if present will also cause the use of
   * the ext format.
   */
  @JvmOverloads
  fun addParameter(
      key: String,
      value: String,
      charset: Charset = Charsets.ISO_8859_1,
      lang: String? = null
  ): ContentDispositionHeaderBuilder {
    return addParameter(key to value, charset, lang)
  }

  /**
   * Adds the provided key-value pair to the Content Disposition's parameters.
   * If `charset` is anything other than [Charsets.ISO_8859_1], the
   * value is encoded in the ext format, and provided as `key*=ext-value`.
   * Lang may optionally be provided, and if present will also cause the use of
   * the ext format.
   */
  @JvmSynthetic
  fun addParameter(
      parameter: Pair<String, String>,
      charset: Charset = Charsets.ISO_8859_1,
      lang: String? = null
  ): ContentDispositionHeaderBuilder {
    return addParameter(parameter, charset, lang, false)
  }

  /**
   * Adds the provided key-value pair to the Content Disposition's parameters.
   * If `charset` is anything other than [Charsets.ISO_8859_1], the
   * value is encoded in the ext format, and provided as `key*=ext-value`.
   * Lang may optionally be provided, and if present will also cause the use of
   * the ext format.
   *
   * This is an internal-only builder method that allows for the specification
   * of an explicit ext flag, only relevant for parsing.
   */
  internal fun addParameter(
      parameter: Pair<String, String>,
      charset: Charset = Charsets.ISO_8859_1,
      lang: String? = null,
      explicitExt: Boolean
  ): ContentDispositionHeaderBuilder {
    val normalizedKey = lowercaseIfFilename(parameter.first)
    parameters.add(
        ContentDispositionHeader.Parameter(
            normalizedKey,
            parameter.second,
            charset,
            lang,
            explicitExt
        )
    )
    return this
  }

  fun build(): ContentDispositionHeader {
    return ContentDispositionHeader(dispositionType, parameters)
  }
}

interface ContentDispositionHeaderDsl {
  /**
   * Adds the provided filename-value pair to the Content Disposition's
   * parameters. If `charset` is anything other than [Charsets.ISO_8859_1],
   * the value is encoded in the ext format, and provided as `filename*=ext-value`.
   * Lang may optionally be provided, and if present will also cause the use of
   * the ext format.
   */
  fun filename(
      value: String,
      charset: Charset = Charsets.ISO_8859_1,
      lang: String? = null
  ) {
    parameter(FILENAME_KEY to value, charset, lang)
  }

  /**
   * Adds the provided key-value pair to the Content Disposition's parameters.
   * If `charset` is anything other than [Charsets.ISO_8859_1], the
   * value is encoded in the ext format, and provided as `key*=ext-value`.
   * Lang may optionally be provided, and if present will also cause the use of
   * the ext format.
   */
  fun parameter(
      parameter: Pair<String, String>,
      charset: Charset = Charsets.ISO_8859_1,
      lang: String? = null
  )
}

// By having this class separate from the above, and private, it allows for
// internal-only functions to stay hidden to the end user, such as `toHeader`.
private class ContentDispositionHeaderDslImpl(dispositionType: String) :
    ContentDispositionHeaderDsl {
  private val builder: ContentDispositionHeaderBuilder =
      ContentDispositionHeader.builder(dispositionType)

  constructor(dispositionType: DispositionType) : this(dispositionType.value)

  override fun parameter(
      parameter: Pair<String, String>,
      charset: Charset,
      lang: String?
  ) {
    builder.addParameter(parameter, charset, lang)
  }

  fun toHeader() = builder.build()
}
