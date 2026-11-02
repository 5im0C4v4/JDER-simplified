import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
val javaVersion = JavaVersion.VERSION_11
plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.10"
    kotlin("plugin.serialization") version "1.9.20"
    id("com.diffplug.spotless") version "8.6.0"
}
val platformJarTargets =
    listOf(
        "WindowsX64" to "windows-x64",
        "LinuxX64" to "linux-x64",
        "LinuxArm64" to "linux-arm64",
        "MacosX64" to "macos-x64",
        "MacosArm64" to "macos-arm64",
    )
val platformNativeLibraries =
    mapOf(
        "windows-x64" to "skiko-windows-x64.dll",
        "linux-x64" to "libskiko-linux-x64.so",
        "linux-arm64" to "libskiko-linux-arm64.so",
        "macos-x64" to "libskiko-macos-x64.dylib",
        "macos-arm64" to "libskiko-macos-arm64.dylib",
    )
val allPlatformNativeFiles =
    platformNativeLibraries.values.flatMap { listOf(it, "$it.sha256") }.toSet()
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
}
fun Jar.includeRuntimeClasspath(platform: String? = null) {
    val retainedNativeFiles =
        platform?.let { platformNativeLibraries.getValue(it) }
            ?.let { setOf(it, "$it.sha256") }
    from(
        configurations.runtimeClasspath.get().map { dependency ->
            if (dependency.isDirectory) {
                dependency
            } else {
                zipTree(dependency).matching {
                    if (retainedNativeFiles != null) {
                        exclude { entry ->
                            entry.name in allPlatformNativeFiles && entry.name !in retainedNativeFiles
                        }
                    }
                }
            }
        }
    )
}
tasks.named<Jar>("jar") {
    includeRuntimeClasspath()
}
platformJarTargets.forEach { (taskSuffix, platformName) ->
    tasks.register<Jar>("jar$taskSuffix") {
        archiveFileName.set("JDER-$platformName.jar")
        from(sourceSets.main.get().output)
        includeRuntimeClasspath(platformName)
    }
}
