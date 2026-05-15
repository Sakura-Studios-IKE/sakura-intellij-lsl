import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.16.0"
    id("org.jetbrains.changelog") version "2.2.1"
}

// Reads CHANGELOG.md (Keep-a-Changelog style) and exposes
// `changelog.renderItem(changelog.getLatest())` for plugin.xml's
// <change-notes>. New entries land in [Unreleased]; the release pipeline
// promotes them to a numbered version.
changelog {
    version.set(providers.gradleProperty("pluginVersion"))
    path.set(file("CHANGELOG.md").canonicalPath)
    header.set(provider { "${version.get()} — ${org.jetbrains.changelog.date()}" })
    headerParserRegex.set(Regex("""^v?\d+\.\d+\.\d+.*$"""))
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("Unreleased")
    groups.set(listOf("Added", "Changed", "Fixed", "Removed", "Deprecated", "Security"))
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(providers.gradleProperty("javaVersion").get().toInt())
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Prefer the maintainer's locally-installed IDE when set via
        //   -PlocalIdePath=/path/to/IDE
        // or via the SAKURA_LSL_IDE env var. This is the most reliable
        // way to build against an EAP / very-recent release whose
        // artefacts aren't on JetBrains' Maven mirror yet, and makes
        // the runIde sandbox identical to the maintainer's IDE.
        val explicit = (project.findProperty("localIdePath") as String?)
            ?: System.getenv("SAKURA_LSL_IDE")
        if (!explicit.isNullOrBlank()) {
            local(explicit)
        } else {
            val type = providers.gradleProperty("platformType").get()
            val version = providers.gradleProperty("platformVersion").get()
            // IntelliJ Platform Gradle plugin 2.x: IDEA Community is no
            // longer published to Maven since 253; use the typed helpers.
            when (type) {
                "IC" -> intellijIdeaCommunity(version)
                "IU" -> intellijIdeaUltimate(version)
                else -> create(type, version)
            }
        }
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        id = providers.gradleProperty("pluginGroup")
        name = "Sakura LSL"
        version = providers.gradleProperty("pluginVersion")

        // <change-notes> in the packaged plugin.xml is generated at build
        // time from CHANGELOG.md — single source of truth.
        changeNotes = provider {
            with(changelog) {
                renderItem(
                    (getOrNull(providers.gradleProperty("pluginVersion").get())
                        ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    org.jetbrains.changelog.Changelog.OutputType.HTML
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    pluginVerification {
        ides {
            // verifyPlugin's `recommended()` downloads 4+ multi-GB IDEs and
            // saturates the GitHub runner's disk/heap. Keep it empty here so
            // CI just builds the zip; run `./gradlew verifyPlugin` locally
            // before each release for cross-version sanity checking.
        }
    }

    // Publishing to JetBrains Marketplace. Requires:
    //   PUBLISH_TOKEN          — Marketplace API token (Settings → My Tokens)
    //   CERTIFICATE_CHAIN      — full plugin signing certificate (PEM)
    //   PRIVATE_KEY            — signing key (PEM)
    //   PRIVATE_KEY_PASSWORD   — passphrase for the signing key
    // Set these as repository secrets; the CI workflow injects them.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey       = providers.environmentVariable("PRIVATE_KEY")
        password         = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token   = providers.environmentVariable("PUBLISH_TOKEN")
        // "default" channel is public; tag a pre-release with -beta etc.
        // to push to a `beta` channel before flipping the bits.
        channels = providers.gradleProperty("pluginVersion").map { ver ->
            listOf(if (ver.contains('-')) ver.substringAfter('-').substringBefore('.') else "default")
        }
    }
}

tasks {
    wrapper {
        gradleVersion = "9.0"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(
                providers.gradleProperty("javaVersion").get()))
            freeCompilerArgs.set(listOf("-Xjsr305=strict"))
        }
    }

    withType<JavaCompile> {
        sourceCompatibility = providers.gradleProperty("javaVersion").get()
        targetCompatibility = providers.gradleProperty("javaVersion").get()
    }
}
