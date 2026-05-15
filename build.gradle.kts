import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.16.0"
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
            create(
                providers.gradleProperty("platformType").get(),
                providers.gradleProperty("platformVersion").get()
            )
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

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    pluginVerification {
        ides {
            recommended()
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
