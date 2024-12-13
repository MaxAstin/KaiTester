import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.serialization)
}

group = "com.tester.kai"
version = "0.1.0"

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

    intellijPlatform {
        create(
            type = IntelliJPlatformType.AndroidStudio,
            version = "2024.1.2.13",
            useInstaller = true
        )

        instrumentationTools()

        bundledPlugin("org.jetbrains.kotlin")
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
}

configurations.all {
    resolutionStrategy.sortArtifacts(ResolutionStrategy.SortOrder.CONSUMER_FIRST)
}
