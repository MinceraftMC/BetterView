import dev.booky.betterview.gradle.BetterViewVersionExt

plugins {
    id("betterview.fabric-intermediary")
}

dependencies {
    // update kyori option library as adventure platform includes a version too old for configurate to work
    include(libs.option)
}

configure<BetterViewVersionExt> {
    versionName = "1.21.3"
}
