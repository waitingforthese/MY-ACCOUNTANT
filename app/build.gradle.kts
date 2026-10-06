plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
//    id("org.jetbrains.kotlin.android.extensions")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-parcelize")
}

android {
    compileSdk = 35
    namespace = "rahul.jagtap.dmas"

    defaultConfig {
        applicationId = "rahul.jagtap.myaccountant"
        minSdk = 23
        targetSdk = 35
        versionCode = 34
        versionName = "3.1.3"
        multiDexEnabled = true
//        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildFeatures.buildConfig = true
        buildConfigField("String", "BASE_URL", "\"https://maha-e-suvidha-default-rtdb.firebaseio.com/\"")
        buildConfigField("String", "OAUTH_CLIENT_ID", "\"408121252150-7fj5uevmmt55pafg1hfeb4gevhh5sj9v.apps.googleusercontent.com\"")
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isZipAlignEnabled = true
            isDebuggable = false
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes.addAll(listOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt"
            ))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
//    lintOptions {
//        checkReleaseBuilds = false
//        // Or, if you prefer, you can continue to check for errors in release builds,
//        // but continue the build even when errors are found:
//        abortOnError = false
//    }
    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                substitute(module("org.hamcrest:hamcrest-core:1.1"))
                    .using(module("junit:junit:4.10"))
            }
            force("com.google.guava:guava:30.1.1-android")
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.1")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.annotation:annotation:1.3.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.2")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation(files("libs/poi-3.7.jar"))
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
//    kapt("com.github.bumptech.glide:compiler:4.11.0")
    implementation("io.github.inflationx:calligraphy3:3.1.1")
    implementation("com.facebook.stetho:stetho:1.5.1")
    implementation("id.zelory:compressor:3.0.1")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.android.support:multidex:1.0.3")
    implementation("de.hdodenhof:circleimageview:3.1.0")
//    implementation("com.github.tbruyelle:rxpermissions:0.12")
    implementation("com.github.jkwiecien:EasyImage:3.2.0")
    implementation("io.reactivex.rxjava3:rxjava:3.0.4")
    //    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // Material Libraries
    // rengwuxian MaterialEditText removed 2026-07-05 — migrated app-wide to Material Outlined TextInputLayout
    implementation("com.afollestad.material-dialogs:commons:0.9.6.0")

    // Firebase
    implementation("com.google.firebase:firebase-analytics:20.1.0")
    implementation("com.google.firebase:firebase-crashlytics:18.2.8")
    implementation("com.google.firebase:firebase-auth-ktx:21.0.1")
    implementation("com.google.firebase:firebase-database-ktx:20.0.3")
    implementation("com.google.firebase:firebase-storage-ktx:20.0.0")
    implementation("com.google.firebase:firebase-messaging:23.0.0")
    implementation("com.google.firebase:firebase-config:22.0.0")

    implementation("io.github.inflationx:calligraphy3:3.1.1")
    implementation("io.github.inflationx:viewpump:2.0.3")

    //Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.8.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.8.0")
    implementation("javax.annotation:jsr250-api:1.0")

    implementation("com.wdullaer:materialdatetimepicker:4.2.3")

    implementation("commons-io:commons-io:2.11.0")

    implementation("me.rosuh:AndroidFilePicker:0.75")

//    implementation 'com.google.android.gms:play-services-ads:20.5.0'

    implementation("jp.wasabeef:blurry:4.0.1")
    implementation("jp.wasabeef:glide-transformations:4.3.0")

    implementation("com.android.volley:volley:1.2.1")

    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation("com.github.wwdablu:SimplyPDF:2.0.0")

    implementation("com.itextpdf:itextg:5.5.10")

//    implementation 'com.github.telichada:SearchableMultiSelectSpinner:2.0'
    implementation("joda-time:joda-time:2.1")

    // Date Range Picker
    implementation("com.borax12.materialdaterangepicker:library:2.0")

    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.13.0")

    implementation("com.googlecode.json-simple:json-simple:1.1.1")

    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.api-client:google-api-client-android:1.32.1") {
        exclude(group = "com.google.guava", module = "guava-jre")
    }
    implementation("com.google.http-client:google-http-client-gson:1.44.2") {
        exclude(group = "com.google.guava", module = "guava-jre")
    }
    implementation("com.google.guava:guava:30.1.1-android")
//    implementation 'com.google.apis:google-api-services-people:v1-rev20210806-1.32.1'
//    implementation 'com.google.api-client:google-api-client:2.0.0'
//    implementation 'com.google.oauth-client:google-oauth-client-jetty:1.34.1'
    implementation("com.google.apis:google-api-services-people:v1-rev20220531-2.0.0")
//    implementation("com.google.apis:google-api-services-youtube:v3-rev20231011-2.0.0")

    // https://mvnrepository.com/artifact/com.google.api-client/google-api-client-jackson2
    implementation("com.google.api-client:google-api-client-jackson2:2.6.0")

    implementation(files("libs/tagsoup-1.2.1.jar"))

    implementation("androidx.work:work-runtime-ktx:2.9.1")

//    implementation("androidx.credentials:credentials:1.5.0")
//    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
//    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
//    implementation("com.google.android.gms:play-services-auth:21.4.0")

//    implementation("com.akexorcist:snap-time-picker:1.0.3")

    //    implementation("androidx.credentials:credentials:1.2.2")
//    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
//    implementation("com.google.android.libraries.identity.googleid:googleid:1.0.0")

    // Credential Manager + Google Identity
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation("junit:junit:4.13")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core-ktx:1.6.0")
    }
}
