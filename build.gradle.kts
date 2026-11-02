import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
val javaVersion = JavaVersion.VERSION_11
plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.10"
    kotlin("plugin.serialization") version "1.9.20"
    id("com.diffplug.spotless") version "8.6.0"
}
group = "com.jder"
repositories {
    mavenCentral()
}
dependencies {
    implementation(compose.desktop.common)
    implementation(compose.desktop.linux_x64)
    implementation(compose.desktop.linux_arm64)
    implementation(compose.desktop.windows_x64)
    implementation(compose.desktop.macos_x64)
    implementation(compose.desktop.macos_arm64)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
}
compose.desktop {
    application {
        mainClass = "com.jder.MainKt"
        nativeDistributions {
            macOS {
                iconFile.set(project.file("src/main/resources/jder_icon.png"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/jder_icon.png"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/jder_icon.png"))
            }
        }
    }
}
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}
spotless {
    kotlin {
        ktfmt().kotlinlangStyle()
    }
}
tasks.withType<KotlinCompile>().configureEach {
    dependsOn("clean", "spotlessApply")
    kotlinOptions {
        jvmTarget = javaVersion.toString()
        freeCompilerArgs += listOf("-Xjvm-default=all", "-Xjdk-release=$javaVersion")
    }
}
tasks.withType<Jar>().configureEach {
    manifest {
        attributes["Main-Class"] = "com.jder.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
