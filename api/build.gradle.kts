dependencies {
    compileOnlyApi(libs.jspecify)
    compileOnlyApi(libs.jetbrains.annotations)
    compileOnlyApi(libs.adventure.key)
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

configure<JavaPluginExtension> {
    withJavadocJar()
}
