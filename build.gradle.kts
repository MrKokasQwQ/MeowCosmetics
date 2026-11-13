plugins {
    id("net.fabricmc.fabric-loom") version "1.17.19"
}

group = "dev.meowcosmetics"
version = project.property("mod_version").toString()
base { archivesName.set("MeowCosmetics-mc${project.property("minecraft_version")}") }

repositories {
    maven("https://maven-central.storage-download.googleapis.com/maven2")
    maven("https://repo1.maven.org/maven2")
    exclusiveContent {
        forRepository { maven("https://maven.fabricmc.net") }
        filter { includeGroup("net.fabricmc"); includeGroup("net.fabricmc.fabric-api") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
}

tasks.withType<JavaCompile>().configureEach { options.release.set(21); options.encoding = "UTF-8" }
java { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }

tasks.processResources {
    inputs.property("version", version)
    filesMatching("fabric.mod.json") { expand("version" to version) }
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "meowcosmetics-test"
        enableGameTests = false
        enableClientGameTests = true
        eula = true
    }
}
