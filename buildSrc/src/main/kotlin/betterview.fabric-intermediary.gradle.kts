import dev.booky.betterview.gradle.BetterViewFabricExt

// Created by booky10 in BetterView (6:21 PM 04.04.2026)

plugins {
    id("betterview.fabric-ext")
    net.fabricmc.`fabric-loom-remap`
    id("betterview.fabric-common")
}

val bvFabricExt = project.extensions.getByType<BetterViewFabricExt>()

dependencies {
    bvFabricExt.afterEvaluate.add {
        minecraft("com.mojang:minecraft:${bvFabricExt.versionName.get()}")

        // depend on moonrise for chunk loading stuff
        val depVersion = bvFabricExt.dependencyVersion().get()
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
