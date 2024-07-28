import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    kotlin("plugin.serialization") version "2.0.0"
    id("app.cash.sqldelight") version "2.0.0"
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.serialization.json)

            implementation("com.squareup.okio:okio:3.9.0")

            implementation("io.github.vinceglb:filekit-core:0.7.0")
            implementation("io.github.vinceglb:filekit-compose:0.7.0")

            api("io.github.qdsfdhvh:image-loader:1.8.2")
            // optional - Compose Multiplatform Resources Decoder
             api("io.github.qdsfdhvh:image-loader-extension-compose-resources:1.8.2")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
        }
        desktopMain.dependencies {
            api("io.github.qdsfdhvh:image-loader-extension-imageio:1.8.2")
            implementation(compose.desktop.currentOs)
            implementation("app.cash.sqldelight:sqlite-driver:2.0.0")
        }
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("io.silv.hmm")
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.silv.hmm"
            packageVersion = "1.0.0"
            windows {
                console = true
            }
        }
    }
}
