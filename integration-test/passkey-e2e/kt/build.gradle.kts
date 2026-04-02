plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
}

kotlin {
    js(IR) {
        browser {
            webpackTask {
                mainOutputFileName = "passkey-e2e.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":auth-kt"))
                implementation(project(":supabase-kt"))
            }
        }
    }
}
