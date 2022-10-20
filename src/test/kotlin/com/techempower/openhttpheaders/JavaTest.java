package com.techempower.openhttpheaders;

import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public class JavaTest {
  static {
    // Note that this class is unused on purpose. It only serves to verify that Java can access the
    // functions of this library in the expected ways, so if this class compiles, the "test" passes.
    MediaType.builder("text", "html").build();
    AcceptHeader.parse("text/html");
    AcceptHeader.parse("text/html", "qs");
    AcceptHeader.builder()
        .addMediaType(
            MediaType.builder("text", "html")
                .addParameter("charset", "utf-8")
                .build()
        )
        .build();
    ContentDispositionHeader.builder(DispositionType.ATTACHMENT)
        .addParameter("test", "case")
        .build();
    ContentDispositionHeader.builder("test").build();
    ContentDispositionHeader.parse("attachment");
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
        .toHeaderString();
    AcceptHeader.parse("text/xml;charset=utf-8;q=0.5,text/html");
    AcceptHeader.of(
        MediaType.of("text", "xml")
            .addParameter("charset", "utf-8")
            .quality(0.5)
    );
    ContentDispositionHeader.of(DispositionType.ATTACHMENT)
        .filename("file.txt")
        .filename("file.txt", StandardCharsets.UTF_8, "en")
        .addParameter("example", "value")
        .addParameter("example", "value", StandardCharsets.UTF_8, "en");
  }
}
