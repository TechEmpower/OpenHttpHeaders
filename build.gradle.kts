import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.7.10"
  id("org.jetbrains.dokka") version "1.7.10"
  `maven-publish`
  signing
  id("com.adarshr.test-logger") version "3.2.0"
}

repositories {
  mavenCentral()
}

val kotestVersion = "5.5.1"

dependencies {
  testImplementation(kotlin("test"))
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
  testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

java {
  withJavadocJar()
  withSourcesJar()
}

group = "com.techempower"
version = "0.1-SNAPSHOT"

publishing {
  publications {
    create<MavenPublication>("maven") {
      pom {
        description.set("This library provides first-party support to Kotlin and Java for HTTP header parsing and creation.")
        url.set("https://github.com/TechEmpower/OpenHttpHeaders")
        licenses {
          license {
            name.set("Revised BSD License, 3-clause")
            url.set("https://opensource.org/licenses/BSD-3-Clause")
            distribution.set("repo")
          }
        }
        developers {
          developer {
            id.set("ajohnstonte")
            name.set("Albert Johnston")
            email.set("ajohnston@techempower.com")
            organization {
              name.set("Techempower")
              url.set("https://www.techempower.com")
            }
            roles.add("Senior Developer")
            timezone.set("America/Los_Angeles")
          }
        }
        scm {
          connection.set("scm:git:https://github.com/TechEmpower/OpenHttpHeaders.git")
          developerConnection.set("scm:git:git@github.com:TechEmpower/OpenHttpHeaders.git")
          url.set("https://github.com/TechEmpower/OpenHttpHeaders.git")
        }
      }
      from(components["java"])
    }
  }

  repositories {
    maven {
      val releasesRepoUrl =
          uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
      val snapshotsRepoUrl =
          uri("https://oss.sonatype.org/content/repositories/snapshots")
      url = if (version.toString()
              .endsWith("SNAPSHOT")
      ) snapshotsRepoUrl else releasesRepoUrl

      name = project.name
      credentials(PasswordCredentials::class)
    }
  }
}

signing {
  // TODO: useGpgCmd() is necessary because it isn't happy with my
  //  pubring.kbx file, and I can't seem to figure out how to produce a
  //  secring.gpg file that it's happy with (in GPG 2.1 they moved away from
  //  that file, which the docs cover but it didn't work for me:
  //  https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials)
  useGpgCmd()
  sign(publishing.publications)
}
