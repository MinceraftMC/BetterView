import dev.booky.betterview.gradle.BetterViewVersionExt

plugins {
    id("betterview.fabric-official")
}

dependencies {
    // required to compile as some of our dependencies use fabric-api, which injects into certain vanilla classes
    compileOnly(platform("net.fabricmc.fabric-api:fabric-api:0.146.1+26.1.2"))
    compileOnly("net.fabricmc.fabric-api:fabric-networking-api-v1")
}

configure<BetterViewVersionExt> {
    versionName = "26.1.2"
    languageVersion = JavaLanguageVersion.of(25)
}
