import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    io.papermc.paperweight.userdev
}

dependencies {
    implementation(projects.paperCommon)
    paperweight.paperDevBundle(libs.versions.paper.v1215)
}

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
