import java.net.URL
import java.util.zip.ZipFile

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.protectong.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

abstract class DownloadCoreBinariesTask : org.gradle.api.DefaultTask() {
  @get:org.gradle.api.tasks.OutputDirectory
  abstract val jniLibsDir: org.gradle.api.file.DirectoryProperty

  @get:org.gradle.api.tasks.Internal
  abstract val buildTmpDir: org.gradle.api.file.DirectoryProperty

  @org.gradle.api.tasks.TaskAction
  fun download() {
    val xrayVersion = "v26.6.1"
    val t2sVersion = "v2.6.0"
    val abis = listOf("arm64-v8a", "armeabi-v7a", "x86_64")
    
    val jniLibsDirFile = jniLibsDir.get().asFile
    val buildTmpDirFile = buildTmpDir.get().asFile
    
    abis.forEach { abi ->
      val jniDir = java.io.File(jniLibsDirFile, abi)
      if (!jniDir.exists()) {
        jniDir.mkdirs()
      }
      
      // 1. Download Xray
      val xrayDest = java.io.File(jniDir, "libxray.so")
      if (!xrayDest.exists()) {
        val xrayUrl = when (abi) {
          "arm64-v8a" -> "https://github.com/XTLS/Xray-core/releases/download/$xrayVersion/Xray-android-arm64-v8a.zip"
          "armeabi-v7a" -> "https://github.com/XTLS/Xray-core/releases/download/$xrayVersion/Xray-linux-arm32-v7a.zip"
          else -> "https://github.com/XTLS/Xray-core/releases/download/$xrayVersion/Xray-linux-64.zip"
        }
        val tempZipFile = java.io.File(buildTmpDirFile, "xray-$abi.zip")
        tempZipFile.parentFile.mkdirs()
        
        println("Downloading official Xray binary for $abi from $xrayUrl ...")
        try {
          java.net.URL(xrayUrl).openStream().use { input ->
            tempZipFile.outputStream().use { output ->
              input.copyTo(output)
            }
          }
          println("Unzipping xray binary...")
          val zipFile = java.util.zip.ZipFile(tempZipFile)
          val entry = zipFile.getEntry("xray")
          if (entry != null) {
            zipFile.getInputStream(entry).use { entryInput ->
              xrayDest.outputStream().use { entryOutput ->
                entryInput.copyTo(entryOutput)
              }
            }
            println("Successfully saved Xray binary to ${xrayDest.absolutePath}")
          } else {
            throw GradleException("Could not find 'xray' in ZIP file for $abi")
          }
          zipFile.close()
        } catch (e: Exception) {
          println("Error downloading Xray for $abi: ${e.message}")
          throw e
        } finally {
          if (tempZipFile.exists()) tempZipFile.delete()
        }
      } else {
        println("Xray binary for $abi already exists. Skipping.")
      }

      // 2. Download tun2socks
      val t2sDest = java.io.File(jniDir, "libtun2socks.so")
      if (!t2sDest.exists()) {
        val t2sUrl = when (abi) {
          "arm64-v8a" -> "https://github.com/xjasonlyu/tun2socks/releases/download/$t2sVersion/tun2socks-linux-arm64.zip"
          "armeabi-v7a" -> "https://github.com/xjasonlyu/tun2socks/releases/download/$t2sVersion/tun2socks-linux-armv7.zip"
          else -> "https://github.com/xjasonlyu/tun2socks/releases/download/$t2sVersion/tun2socks-linux-amd64.zip"
        }
        val tempZipFile = java.io.File(buildTmpDirFile, "t2s-$abi.zip")
        tempZipFile.parentFile.mkdirs()
        
        println("Downloading official tun2socks binary for $abi from $t2sUrl ...")
        try {
          java.net.URL(t2sUrl).openStream().use { input ->
            tempZipFile.outputStream().use { output ->
              input.copyTo(output)
            }
          }
          println("Unzipping tun2socks binary...")
          val zipFile = java.util.zip.ZipFile(tempZipFile)
          val binaryName = when (abi) {
            "arm64-v8a" -> "tun2socks-linux-arm64"
            "armeabi-v7a" -> "tun2socks-linux-armv7"
            else -> "tun2socks-linux-amd64"
          }
          val entry = zipFile.getEntry(binaryName)
          if (entry != null) {
            zipFile.getInputStream(entry).use { entryInput ->
              t2sDest.outputStream().use { entryOutput ->
                entryInput.copyTo(entryOutput)
              }
            }
            println("Successfully saved tun2socks binary to ${t2sDest.absolutePath}")
          } else {
            throw GradleException("Could not find '$binaryName' in ZIP file for $abi")
          }
          zipFile.close()
        } catch (e: Exception) {
          println("Error downloading tun2socks for $abi: ${e.message}")
          throw e
        } finally {
          if (tempZipFile.exists()) tempZipFile.delete()
        }
      } else {
        println("tun2socks binary for $abi already exists. Skipping.")
      }
    }
  }
}

tasks.register<DownloadCoreBinariesTask>("downloadCoreBinaries") {
  group = "custom"
  description = "Downloads official Xray-core (v26.6.1) and tun2socks (v2.6.0) binaries from GitHub releases"
  jniLibsDir.set(layout.projectDirectory.dir("src/main/jniLibs"))
  buildTmpDir.set(layout.projectDirectory.dir("build/tmp"))
}

tasks.named("preBuild") {
  dependsOn("downloadCoreBinaries")
}
