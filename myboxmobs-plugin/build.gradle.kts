plugins {
    `java-library`
}

val paperApi = "io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT"

dependencies {
    implementation(project(":myboxmobs-api"))
    compileOnly(paperApi)
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        isTransitive = false
    }
    // Provided by their plugins at runtime, never shaded
    compileOnly(files("../libs/MythicMobs-5.13.0.jar"))
    compileOnly(files("../libs/ModelEngine-R4.1.1.jar"))
    compileOnly(files("../libs/DecentHolograms-2.10.1.jar"))
    compileOnly(files("../libs/boxcore-api-1.3.0.jar"))

    testImplementation(paperApi)
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveFileName.set("MyBoxMobs-${project.version}.jar")
    val apiJar = project(":myboxmobs-api").tasks.named<Jar>("jar")
    dependsOn(apiJar)
    from(apiJar.map { zipTree(it.archiveFile) }) {
        exclude("META-INF/MANIFEST.MF")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}
