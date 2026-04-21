import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.PublishModTask

// Created by booky10 in BetterView (2:31 PM 21.04.2026)

plugins {
    me.modmuss50.`mod-publish-plugin`
}

// prioritize shadow jar over default jar task
val jarTaskProvider: Provider<Jar> = gradle.providers.provider {
    if (tasks.findByName("shadowJar") != null) {
        return@provider tasks.named<Jar>("shadowJar")
    }
    return@provider tasks.named<Jar>("jar")
}.flatMap { it }

configure<ModPublishExtension> {
    changelog = providers.environmentVariable("CHANGELOG")
        .map { it.trim() }
        .getOrElse("No changelog provided")
    type = STABLE
    dryRun = !hasProperty("noDryPublish")

    // if there is no publishing platform set, assume this is the root task
    // which is used to support github releases properly
    if (!hasProperty("publishing.platform")) {
        github {
            accessToken = providers.environmentVariable("GITHUB_API_TOKEN")
            repository = providers.environmentVariable("GITHUB_REPOSITORY")
            commitish = providers.environmentVariable("GITHUB_REF_NAME")
            tagName = version.map { "v${it}" }
            displayName = version

            // won't be empty, just so the publishing plugin won't complain
            allowEmptyFiles = true
        }

        // iterate through all subprojects and search for configured release files
        subprojects.filter { it.hasProperty("publishing.platform") }.forEach {
            // add main platform publishing file after project has finished evaluating
            it.afterEvaluate {
                additionalFiles.from(publishMods.file)
            }
        }
    } else {
        // setup files for this platform
        file = jarTaskProvider.flatMap { it.archiveFile }

        val platform = property("publishing.platform").toString()
        val fancyPlatform = platform.replaceFirstChar { it.titlecaseChar() }

        // setup per-platform modrinth publishing
        modrinth {
            accessToken = providers.environmentVariable("MODRINTH_API_TOKEN")
            version = "${project.version}+$platform"
            displayName = "${rootProject.ext["publishing.display_name"]} ${project.version} ${fancyPlatform}"
            projectId = property("publishing.modrinth.project").toString()
            minecraftVersionRange {
                start = property("publishing.modrinth.version.start").toString()
                end = property("publishing.modrinth.version.end").toString()
            }
            val loadersStr = property("publishing.modrinth.loaders").toString()
            modLoaders.addAll(loadersStr.split(","))
        }
    }
}

tasks.withType<PublishModTask> {
    dependsOn(jarTaskProvider)
}
