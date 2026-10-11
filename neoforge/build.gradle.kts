plugins {
    java
    id("net.neoforged.moddev") version ("2.0.148")
}

version = property("mod_version")!!
group = "com.hbm.dhcompat"
base.archivesName = "ntm-dh-compat-neoforge"

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

sourceSets.main {
    java.srcDir("../common/src/main/java")
    resources.srcDir("../common/src/main/resources")
}

neoForge {
    version = property("neoforge_version").toString()
    runs { register("client") { client() } }
    mods { register("ntm_dh_compat") { sourceSet(sourceSets.main.get()) } }
}

repositories {
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
    maven("https://repo.warfactory.co/releases") { content { includeGroup("com.hbm"); includeGroup("dev.engine_room") } }
}

dependencies {
    compileOnly("maven.modrinth:distanthorizons:3.3.3-26.2")
    // dev client only: the game this addon runs in
    runtimeOnly("com.hbm:ntm-next-neoforge:1.0.2+mc26.2")
    runtimeOnly("dev.engine_room:crankshaft-neoforge:1.5.10+mc26.2")
    runtimeOnly("maven.modrinth:distanthorizons:3.3.3-26.2")
}

tasks.processResources {
    inputs.property("version", version)
    filesMatching("META-INF/neoforge.mods.toml") { expand("version" to version) }
}
