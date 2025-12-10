import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.pluginyml.bukkit)
    alias(libs.plugins.runtask.paper)
}

dependencies {
    compileOnly(libs.paper.api.base)

    // include paper common subproject
    implementation(projects.paperCommon)
    // include paper nms subprojects
    rootProject.subprojects
        .filter { it.name.matches("^paper-\\d+$".toRegex()) }
        .forEach { runtimeOnly(it) }

    // download caffeine caching library on runtime
    library(libs.caffeine)
}

tasks.withType<Jar> {
    manifest.attributes(
        mapOf(
            "paperweight-mappings-namespace" to "mojang"
        )
    )
    // this isn't a fabric mod, exclude this
    exclude("fabric.mod.json")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
    // final paper jar, place it in root build dir
    destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
    archiveClassifier = ""
    // relocate shaded dependencies
    mapOf(
        "org.bstats" to "bstats"
    ).forEach { (key, value) ->
        relocate(key, "${project.group}.libs.$value")
    }
}

tasks.named("assemble") {
    dependsOn(tasks.withType<ShadowJar>())
}

configure<BukkitPluginDescription> {
    name = rootProject.name
    main = "${project.group}.BetterViewPlugin"
    description = "Extends the normal server view distance without sacrificing server performance"
    authors = listOf("booky10")
    website = "https://minceraft.dev/betterview"
    apiVersion = "1.21.1"
}

tasks.named<RunServer>("runServer") {
    val version = "1.21.11"
    minecraftVersion(version)
    runDirectory = runDirectory.get().dir(version)
}
