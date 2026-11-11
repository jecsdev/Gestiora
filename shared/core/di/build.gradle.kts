import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.jecsdev.gestiora.core_di"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:common"))
            implementation(project(":shared:core:ui"))
            implementation(project(":shared:data:datasources-core"))
            implementation(project(":shared:data:repository"))
            implementation(project(":shared:domain:usecases"))
            implementation(project(":shared:presentation:viewmodels"))
            implementation(libs.koin.core)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}

