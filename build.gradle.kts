plugins {
    kotlin("multiplatform") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
}

group = "dev.josejordan"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("io.ktor:ktor-client-core:2.3.10")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.10")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.10")
                implementation("io.ktor:ktor-client-darwin:2.3.10")
                implementation("com.squareup.okio:okio:3.9.0")
            }
        }

        val nativeTest by getting
        
        // Platform-specific sources and dependencies
        targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
            when (konanTarget.family) {
                org.jetbrains.kotlin.konan.target.Family.OSX -> {
                    compilations.getByName("main") {
                        defaultSourceSet.dependencies {
                            implementation("io.ktor:ktor-client-darwin:2.3.10")
                        }
                    }
                }
                org.jetbrains.kotlin.konan.target.Family.MINGW -> {
                    compilations.getByName("main") {
                        defaultSourceSet.dependencies {
                            implementation("io.ktor:ktor-client-winhttp:2.3.10")
                        }
                    }
                }
                org.jetbrains.kotlin.konan.target.Family.LINUX -> {
                    compilations.getByName("main") {
                        defaultSourceSet.dependencies {
                            implementation("io.ktor:ktor-client-cio:2.3.10")
                        }
                    }
                }
                else -> {
                    // Fallback to CIO for other platforms
                    compilations.getByName("main") {
                        defaultSourceSet.dependencies {
                            implementation("io.ktor:ktor-client-cio:2.3.10")
                        }
                    }
                }
            }
        }
    }
}