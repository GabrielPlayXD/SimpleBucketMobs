version = "1.2"

plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.8"
    id("xyz.jpenilla.run-paper") version "2.3.1" // Adds runServer and runMojangMappedServer tasks for testing
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.glaremasters.me/repository/bloodshot")
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.griefdefender:api:2.1.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    javadoc {
        options.encoding = "UTF-8"
    }
    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}