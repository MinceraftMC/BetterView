import dev.booky.betterview.gradle.BetterViewFabricExt

plugins {
    id("betterview.fabric-intermediary")
}

dependencies {
    // update kyori option library as adventure platform includes a version too old for configurate to work
    include(libs.option)
}

configure<BetterViewFabricExt> {
    versionName = "1.21.1"
}
