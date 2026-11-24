pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}

dependencyManagement {
    dependencies {
        // Google Jetpack Compose Bill of Materials
        implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    }
}

rootProject.name = "mono-beacon"

include(":app")
