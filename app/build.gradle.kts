plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// ============================================================
// 随机包名生成器 — 每次 build 自动生成伪装成系统组件的包名
// ============================================================
val androidPrefixes = listOf("com.android", "com.google.android", "com.qualcomm", "com.sec")
val sysWords = listOf(
    "fingerprint", "location", "secure", "protect", "guard",
    "system", "service", "provider", "config", "setting",
    "trust", "safety", "verify", "auth", "core", "base",
    "device", "identity", "network", "wifi", "bluetooth",
    "display", "input", "storage", "permission", "privacy"
)

fun generatePkgName(): String {
    val prefix = androidPrefixes.random()
    val count = (2..3).random()
    val words = (1..count).map { sysWords.random() }.joinToString(".")
    return "$prefix.$words"
}

val randomPkgName = generatePkgName()
println("══════════════════════════════════════════")
println("  ZfC 随机包名: $randomPkgName")
println("══════════════════════════════════════════")

android {
    namespace = randomPkgName
    compileSdk = 36

    defaultConfig {
        applicationId = randomPkgName
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["applicationId"] = randomPkgName
        manifestPlaceholders["appLabel"] = "ZfC"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            excludes.add("**/kotlin/**")
            excludes.add("kotlin-tooling-metadata.json")
        }
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.appcompat)
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
}
