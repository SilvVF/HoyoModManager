import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    kotlin("plugin.serialization") version "2.0.0"
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

            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)
        }
        desktopMain.dependencies {
            api("io.github.qdsfdhvh:image-loader-extension-imageio:1.8.2")
            implementation(compose.desktop.currentOs)
            implementation("app.cash.sqldelight:sqlite-driver:2.0.0")
        }
    }
}

dependencies {
    ksp(libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
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
