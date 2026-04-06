import dev.booky.betterview.gradle.BetterViewVersionExt

// Created by booky10 in BetterView (6:21 PM 04.04.2026)

plugins {
    id("betterview.version-ext")
    net.fabricmc.`fabric-loom-remap`
    id("betterview.fabric-common")
}

val betterviewExt = project.extensions.getByType<BetterViewVersionExt>()

dependencies {
    betterviewExt.afterEvaluate.add {
        minecraft("com.mojang:minecraft:${betterviewExt.versionName.get()}")

        // depend on moonrise for chunk loading stuff
        val depVersion = betterviewExt.dependencyVersion().get()
        val moonriseVersion = libs.versions.hackGetVersion("moonrise.$depVersion")
        modApi("maven.modrinth:moonrise-opt:$moonriseVersion")

        // adventure component library
        val adventureVersion = libs.versions.hackGetVersion("adventure.platform.fabric.$depVersion")
        modImplementation("net.kyori:adventure-platform-fabric:$adventureVersion")
    }

    // skip depending on parchment, just way too much
    // of a headache using this plugin-based setup
    mappings(loom.officialMojangMappings())

    modImplementation(libs.fabric.loader)
}
