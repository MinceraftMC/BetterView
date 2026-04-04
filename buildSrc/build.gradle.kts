plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/")
    gradlePluginPortal()
}

dependencies {
    implementation(libs.paperweight)
    implementation(libs.shadow)
    implementation(libs.loom)
    implementation(libs.pluginyml)
    implementation(libs.runtask)
}
