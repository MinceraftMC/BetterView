import dev.booky.betterview.gradle.BetterViewVersionExt

plugins {
    id("betterview.paper")
}

configure<BetterViewVersionExt> {
    versionName = "26.1.1"
    languageVersion = JavaLanguageVersion.of(25)
}
