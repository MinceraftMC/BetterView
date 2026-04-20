import dev.booky.betterview.gradle.BetterViewVersionExt

plugins {
    id("betterview.fabric-official")
}

configure<BetterViewVersionExt> {
    versionName = "26.1.2"
    languageVersion = JavaLanguageVersion.of(25)
}
