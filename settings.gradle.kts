plugins {
    id("com.gradle.develocity") version "3.17.5"
}

rootProject.name = "simple-ocr"

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        uploadInBackground = false // bad for CI, and not critical for local runs
    }
}
