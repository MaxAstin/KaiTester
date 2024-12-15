import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.changelog)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

dependencies {
    implementation(libs.bundles.ktor)
    implementation(libs.kotlinx.serialization)

    testImplementation(libs.junit)

    intellijPlatform {
        create(
            type = IntelliJPlatformType.AndroidStudio,
            version = "2024.1.2.13",
            useInstaller = true
        )

        bundledPlugin("org.jetbrains.kotlin")

        instrumentationTools()
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        description = getPluginDescription()

        val changelog = project.changelog
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = providers.gradleProperty("pluginVersion").map {
            listOf(
                it.substringAfter(
                    delimiter = '-',
                    missingDelimiterValue = ""
                ).substringBefore('.')
                    .ifEmpty { "default" }
            )
        }
    }

    pluginVerification {
        ides {
            ide(
                type = IntelliJPlatformType.AndroidStudio,
                version = "2024.1.2.13",
                useInstaller = true
            )
            recommended()
        }
    }
}

configurations.runtimeOnly {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
}

tasks {
    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    register("validateOpenAIKey") {
        doLast {
            val apiKey = System.getenv("OPENAI_API_KEY")
            if (apiKey.isNullOrEmpty()) {
                throw GradleException("OPENAI_API_KEY environment variable is not set")
            }
        }
    }

    buildPlugin {
        dependsOn("validateOpenAIKey")
    }
}

configurations.all {
    resolutionStrategy.sortArtifacts(ResolutionStrategy.SortOrder.CONSUMER_FIRST)
}

fun getPluginDescription(): Provider<String> {
    val readmeFile = layout.projectDirectory.file("README.md")
    return providers.fileContents(readmeFile).asText.map {
        val start = "<!-- Plugin description start -->"
        val end = "<!-- Plugin description end -->"

        with(it.lines()) {
            if (!containsAll(listOf(start, end))) {
                throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
            }
            subList(
                fromIndex = indexOf(start) + 1,
                toIndex = indexOf(end)
            ).joinToString("\n")
                .let(::markdownToHTML)
        }
    }
}
