import dev.booky.betterview.gradle.BetterViewFabricExt

plugins {
    id("betterview.fabric-intermediary")
}

configure<BetterViewFabricExt> {
    versionName = "1.21.4"
}
