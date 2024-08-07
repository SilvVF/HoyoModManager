import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

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

        val voyagerVersion = "1.1.0-beta02"

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.animation)
            implementation(compose.animationGraphics)
            implementation(compose.uiTooling)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.serialization.json)
            
            api(libs.androidx.datastore.preferences.core)
            api(libs.androidx.datastore.core.okio)

            implementation("com.squareup.okio:okio:3.9.0")
            implementation("io.github.vinceglb:filekit-core:0.7.0")

            api("io.github.qdsfdhvh:image-loader:1.8.2")
            // optional - Compose Multiplatform Resources Decoder
            api("io.github.qdsfdhvh:image-loader-extension-compose-resources:1.8.2")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)

            implementation("org.jsoup:jsoup:1.7.2")

            implementation("net.sf.sevenzipjbinding:sevenzipjbinding:16.02-2.01")
            implementation("net.sf.sevenzipjbinding:sevenzipjbinding-all-platforms:16.02-2.01")
            
            implementation("cafe.adriel.voyager:voyager-navigator:$voyagerVersion")
            implementation("cafe.adriel.voyager:voyager-transitions:$voyagerVersion")

            implementation("androidx.core:core-ktx:1.13.1")
            implementation("androidx.room:room-ktx:2.6.1")
        }
        desktopMain.dependencies {
            api("io.github.qdsfdhvh:image-loader-extension-imageio:1.8.2")
            implementation(compose.desktop.currentOs)
        }
    }
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions.freeCompilerArgs.addAll(
        listOf(
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-Xcontext-receivers",
        )
    )
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

            buildTypes.release.proguard {
            }
            windows {
            }
        }
    }
}