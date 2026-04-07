import dev.booky.betterview.gradle.BetterViewVersionExt
import io.papermc.paperweight.userdev.PaperweightUserExtension
import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

// Created by booky10 in BetterView (10:09 PM 05.04.2026)

plugins {
    io.papermc.paperweight.userdev
    id("betterview.version-ext")
}

val betterviewExt = project.extensions.getByType<BetterViewVersionExt>()

dependencies {
    implementation(project(":paper-common"))
    paperweight.paperDevBundle(
        betterviewExt.dependencyVersion()
            .map { libs.versions.hackGetVersion("paper.$it") })
}

configure<PaperweightUserExtension> {
    reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

// hack to allow depending on this platform in java 21 projects
configurations.runtimeElements.configure {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
}
