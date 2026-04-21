import net.fabricmc.loom.task.AbstractRemapJarTask
import net.fabricmc.loom.task.prod.ClientProductionRunTask
import net.fabricmc.loom.task.prod.ServerProductionRunTask

plugins {
    net.fabricmc.`fabric-loom`
    id("betterview.publishing")
}

val testTaskVersion = "26.1.2"
val testTaskJavaVersion = JavaLanguageVersion.of(25)
val testTaskVersionFiltered = testTaskVersion.replace(".", "")

val includeAll: Configuration by configurations.creating

dependencies {
    // dummy fabric env setup (use non-obfuscated version for less prep time)
    minecraft("com.mojang:minecraft:26.1.2")

    // include common projects once
    include(projects.api)
    include(projects.common)

    // include common dependencies
    sequenceOf(libs.caffeine, libs.configurate.yaml).forEach {
        includeAll(it) {
            exclude("net.kyori", "option") // included in adventure platforms
            exclude("com.google.errorprone", "error_prone_annotations") // useless
            exclude("org.jspecify") // useless
        }
    }

    // include all fabric versions
    rootProject.subprojects
        .filter { it.name.matches("^fabric-\\d+$".toRegex()) }
        .forEach { include(it) }

    // version-specific runtime mods
    val moonriseVersion = libs.versions.hackGetVersion("moonrise.v$testTaskVersionFiltered")
    productionRuntimeMods("maven.modrinth:moonrise-opt:$moonriseVersion")
    val adventureVersion = libs.versions.hackGetVersion("adventure.platform.fabric.v$testTaskVersionFiltered")
    productionRuntimeMods("net.kyori:adventure-platform-fabric:$adventureVersion")
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.named<Jar>("jar") {
    // include common dependencies transitively
    fun doInclude(dep: ResolvedDependency) {
        configurations.named("include").get().withDependencies {
            this.add(dependencyFactory.create(dep.moduleGroup, dep.moduleName, dep.moduleVersion))
        }
        dep.children.forEach { doInclude(it) }
    }
    includeAll.resolvedConfiguration.firstLevelModuleDependencies.forEach { doInclude(it) }
    // final fabric jar, place it in root build dir
    destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
}

tasks.withType<AbstractRemapJarTask> {
    archiveBaseName = "${rootProject.name}-${project.name}".lowercase()
}

loom {
    mixin.defaultRefmapName = "${rootProject.name}-${project.name}-refmap.json".lowercase()
}

// fabric's default task doesn't allow us to specify that we want to have standard input
@UntrackedTask(because = "Always rerun this task.")
abstract class CustomServerProductionRunTask : ServerProductionRunTask {

    @Inject
    constructor() : super()

    override fun configureProgramArgs(exec: ExecSpec) {
        super.configureProgramArgs(exec)
        exec!!.standardInput = System.`in`
    }
}

tasks.register<CustomServerProductionRunTask>("prodServer") {
    jvmArgs.add("-Dmixin.debug.export=true")
    minecraftVersion = testTaskVersion
    loaderVersion = libs.versions.fabric.loader.get()
    runDir = project.layout.projectDirectory.dir("run/${testTaskVersion}")
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = testTaskJavaVersion
    }
}

tasks.register<ClientProductionRunTask>("prodClient") {
    jvmArgs.add("-Dmixin.debug.export=true")
    runDir = project.layout.projectDirectory.dir("run/${testTaskVersion}")
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = testTaskJavaVersion
    }
}
