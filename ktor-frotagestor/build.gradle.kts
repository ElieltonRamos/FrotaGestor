plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version "1.9.0"
    id("com.gradleup.shadow") version "8.3.8"
}

group = "com.frotagestor"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.config.yaml)
    implementation("io.ktor:ktor-server-auth:2.3.4")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("org.jetbrains.exposed:exposed-core:0.56.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.56.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.56.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.56.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.flywaydb:flyway-core:10.14.0")
    implementation("org.flywaydb:flyway-mysql:10.14.0")
    implementation("io.ktor:ktor-server-cors:2.3.4")
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

tasks.register<Exec>("packageWin") {
    dependsOn("shadowJar")

    val appName = "FrotaGestor"
    val appVersion = project.version.toString()
    val shadowJar = tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").get()
    val mainJar = shadowJar.archiveFileName.get()
    val mainClass = "io.ktor.server.netty.EngineMain"

    commandLine(
        "jpackage",
        "--name", appName,
        "--app-version", appVersion,
        "--input", "build/libs",
        "--main-jar", mainJar,
        "--main-class", mainClass,
        "--type", "exe",
        "--win-dir-chooser",
        "--win-menu",
        "--win-shortcut"
        // "--icon", "src/main/resources/icon.ico"
    )
}

tasks.register<Exec>("packageLinux") {
    dependsOn("shadowJar")

    val appName = "FrotaGestor"
    val appVersion = project.version.toString()
    val mainJar = "ktor-frotagestor-all.jar"
    val mainClass = "io.ktor.server.netty.EngineMain"

    commandLine(
        "jpackage",
        "--name", appName,
        "--app-version", appVersion,
        "--input", "build/libs",
        "--main-jar", mainJar,
        "--main-class", mainClass,
        "--type", "deb",
        "--linux-shortcut",
        "--linux-menu-group", appName
        // "--icon", "src/main/resources/icon.png" // opcional
    )
}