import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    io.papermc.paperweight.userdev
}

dependencies {
    implementation(projects.paperCommon)
    paperweight.paperDevBundle(libs.versions.paper.v1211)
}

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
