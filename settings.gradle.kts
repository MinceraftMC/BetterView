enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "BetterView"

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
    }
}

include("common")

include("paper-common", "paper")
sequenceOf("1.21.1", "1.21.3", "1.21.4", "1.21.5", "1.21.9", "1.21.11")
    .map { it.replace(".", "") }
    .forEach { include("paper-$it") }

sequenceOf("1.21.1", "1.21.3", "1.21.4", "1.21.5", "1.21.7", "1.21.9", "1.21.11")
    .map { it.replace(".", "") }
    .forEach { include("fabric-$it") }
include("fabric")
