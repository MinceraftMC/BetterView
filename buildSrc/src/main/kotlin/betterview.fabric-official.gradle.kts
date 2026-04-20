import dev.booky.betterview.gradle.BetterViewVersionExt

// Created by booky10 in BetterView (6:21 PM 04.04.2026)

plugins {
    id("betterview.version-ext")
    net.fabricmc.`fabric-loom`
    id("betterview.fabric-common")
}

val betterviewExt = project.extensions.getByType<BetterViewVersionExt>()

dependencies {
    betterviewExt.afterEvaluate.add {
        minecraft("com.mojang:minecraft:${betterviewExt.versionName.get()}")

        // depend on moonrise for chunk loading stuff
        val depVersion = betterviewExt.dependencyVersion().get()
        val moonriseVersion = libs.versions.hackGetVersion("moonrise.$depVersion")
        api("maven.modrinth:moonrise-opt:$moonriseVersion")

        // adventure component library
        val adventureVersion = libs.versions.hackGetVersion("adventure.platform.fabric.$depVersion")
        implementation("net.kyori:adventure-platform-fabric:$adventureVersion")
    }

    implementation(libs.fabric.loader)
}
