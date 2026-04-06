// Created by booky10 in BetterView (6:21 PM 04.04.2026)

import dev.booky.betterview.gradle.BetterViewVersionExt

val betterviewExt = project.extensions.create<BetterViewVersionExt>("betterview")

// we need to load stuff before fabric does, so this plugin
// separates the extension properties from the actual config
project.afterEvaluate {
    betterviewExt.afterEvaluate.forEach { it.run() }
}
