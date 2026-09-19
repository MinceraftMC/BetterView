import dev.booky.betterview.gradle.BetterViewVersionExt

plugins {
    id("betterview.fabric-official")
}

dependencies {
    // required to compile as some of our dependencies use fabric-api, which injects into certain vanilla classes
    compileOnly(platform("net.fabricmc.fabric-api:fabric-api:0.161.0+26.3"))
    compileOnly("net.fabricmc.fabric-api:fabric-networking-api-v1")
}

configure<BetterViewVersionExt> {
    versionName = "26.3"
    languageVersion = JavaLanguageVersion.of(25)
}
