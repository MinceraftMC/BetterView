import dev.booky.betterview.gradle.BetterViewVersionExt

plugins {
    id("betterview.paper")
}

configure<BetterViewVersionExt> {
    versionName = "26.3"
    languageVersion = JavaLanguageVersion.of(25)
}
