pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        gradlePluginPortal()
    }
}

rootProject.name = "ntm-dh-compat"
include("fabric", "neoforge")
