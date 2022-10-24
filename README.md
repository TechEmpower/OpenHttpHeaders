# OpenHTTPHeaders

This library provides first-party support to Kotlin and Java for HTTP header 
parsing and creation. It is not tied to any particular framework, and provides 
first-party support to Kotlin, with second-party support for Java only in that 
it is dependent on the Kotlin standard library (stdlib) but otherwise fully 
supports Java. There are no other dependencies.

### Sample Code

To create an Accept header in Kotlin:

```kotlin
acceptHeader {
  mediaType(type = "text", subtype = "xml") {
    parameters(
      "charset" to "utf-8"
    )
    quality = 0.5
  }
  mediaType(type = "text", subtype = "html")
}.toHeaderString()
// This produces "text/xml;charset=utf-8;q=0.5,text/html"
```

To do the same in Java:

```jshelllanguage
AcceptHeader.builder()
    .addMediaType(
        MediaType.builder("text", "xml")
            .addParameter("charset", "utf-8")
            .quality(0.5)
            .build()
    )
    .addMediaType(
        MediaType.builder("text", "html").build()
    )
    .build()
    .toHeaderString()

// or

AcceptHeader.of(
    MediaType.of("text", "xml")
        .addParameter("charset", "utf-8")
        .quality(0.5)
).toHeaderString()

// These both produce "text/xml;charset=utf-8;q=0.5,text/html"
```

To parse an Accept header, in both languages, call:

```kotlin
AcceptHeader.parse("text/xml;charset=utf-8;q=0.5,text/html")
// This produces the objects described above, specifically:
// AcceptHeader(
//   mediaTypes = [
//     MediaType(
//       type = "text",
//       subtype = "xml",
//       parameters = {
//         "charset": "utf-8",
//         "q": "0.5"
//       },
//       quality = 0.5
//     ),
//     MediaType(
//       type = "text",
//       subtype = "html"
//     )
//   ]
// )
```

## Contributing

### Build and Test

After checking out this project, to build, run:

```shell
./gradlew build
```

To run all tests, run:

```shell
./gradlew cleanTest test
```

Note: `cleanTest` is necessary because Gradle may otherwise detect that no
changes to the tested code have occurred and skip running tests. The above
guarantees that the tests are run when requested. 

### Publish

Add to `~/.gradle/gradle.properties` (or create if it doesn't already exist):

```properties
signing.keyId=<redacted>
# The following will likely be `~/.gnupg/pubring.kbx` or `~/.gnupg/secring.gpg`
# depending on the file present, but it must be written out as a full path (not
# using ~)
signing.secretKeyRingFile=<required>
signing.gnupg.keyName=<redacted>
signing.gnupg.passphrase=<redacted>
openhttpheadersUsername=<redacted>
openhttpheadersPassword=<redacted>
```

Note: `signing.keyId` may not be necessary. Should be checked later.
