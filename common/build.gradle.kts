dependencies {
    api(projects.api)

    // declare everything as compileOnly; if any platforms
    // require a specific dependency to be included, this should
    // be done in the specific platform subproject and not here
    compileOnlyApi(libs.slf4j.api)
    compileOnlyApi(libs.jspecify)
    compileOnlyApi(libs.checker.qual)
    compileOnlyApi(libs.netty.buffer)
    compileOnlyApi(libs.netty.transport)
    compileOnlyApi(libs.fastutil)
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.caffeine)
    compileOnlyApi(libs.configurate.yaml)
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
