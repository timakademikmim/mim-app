plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.serialization")
}

val updateManifestUrl = providers.gradleProperty("MIM_UPDATE_MANIFEST_URL")
  .orElse("https://raw.githubusercontent.com/timakademikmim/mim-app/main/releases/mim-app-update.json")
  .get()
  .replace("\\", "\\\\")
  .replace("\"", "\\\"")
val appVersionCode = providers.gradleProperty("MIM_VERSION_CODE")
  .orElse("1")
  .get()
  .toInt()
val appVersionName = providers.gradleProperty("MIM_VERSION_NAME")
  .orElse("0.1.0")
  .get()

android {
  namespace = "com.mim.guruapp"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.mim.guruapp"
    minSdk = 26
    targetSdk = 36
    versionCode = appVersionCode
    versionName = appVersionName
    buildConfigField("String", "SUPABASE_URL", "\"https://optucpelkueqmlhwlbej.supabase.co\"")
    buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE\"")
    buildConfigField("String", "APP_UPDATE_MANIFEST_URL", "\"$updateManifestUrl\"")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("MIM_ANDROID_KEYSTORE").orEmpty()
      val keystorePassword = System.getenv("MIM_ANDROID_KEYSTORE_PASSWORD").orEmpty()
      val keyAliasValue = System.getenv("MIM_ANDROID_KEY_ALIAS").orEmpty()
      val keyPasswordValue = System.getenv("MIM_ANDROID_KEY_PASSWORD").orEmpty()

      if (keystorePath.isNotBlank()) {
        storeFile = file(keystorePath)
        storePassword = keystorePassword
        keyAlias = keyAliasValue
        keyPassword = keyPasswordValue
      }
    }
  }

  applicationVariants.all {
    outputs.all {
      (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "MIM APP.apk"
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("release")
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.15"
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2024.10.01")

  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.activity:activity-compose:1.9.3")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation("androidx.datastore:datastore-preferences:1.1.1")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")

  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}
