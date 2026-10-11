plugins {
    java
    id("net.fabricmc.fabric-loom") version ("1.18.2")
}

version = property("mod_version")!!
group = "com.hbm.dhcompat"
base.archivesName = "ntm-dh-compat-fabric"

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

sourceSets.main {
    java.srcDir("../common/src/main/java")
    resources.srcDir("../common/src/main/resources")
}

repositories {
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    compileOnly("maven.modrinth:distanthorizons:3.3.3-26.2")
}

tasks.processResources {
    inputs.property("version", version)
    filesMatching("fabric.mod.json") { expand("version" to version) }
}
