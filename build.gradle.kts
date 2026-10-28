import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
    id("org.jetbrains.compose") version "1.9.0"
}

group = "dev.ricardo"
version = "1.1.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.jetbrains.compose.ui:ui-test-junit4-desktop:1.9.0")
}

tasks.test {
    useJUnit()
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Some remote X servers have no working GL; `-PsoftwareRender` forces Skiko's CPU renderer.
tasks.withType<JavaExec>().configureEach {
    if (providers.gradleProperty("softwareRender").isPresent) {
        systemProperty("skiko.renderApi", "SOFTWARE")
    }
}

compose.desktop {
    application {
        mainClass = "dev.ricardo.mdviewer.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Exe)
            packageName = "MDViewer"
            packageVersion = "1.1.0"
            description = "Offline Markdown reader with block-anchored annotations"
            vendor = "Ricardo"
            copyright = "MIT"

            linux {
                iconFile.set(project.file("packaging/icon.png"))
                menuGroup = "Office"
                appCategory = "Office"
                debMaintainer = "Ricardo"
            }

            windows {
                iconFile.set(project.file("packaging/icon.ico"))
                menuGroup = "Office"
                shortcut = true
                perUserInstall = true
                // Markdown file associations are registered by the Inno Setup wizard
                // (packaging/windows/MDViewer.iss), not by jpackage, because Compose 1.9.0
                // marks `fileAssociations` internal on the Windows platform settings.
            }
        }
    }
}
