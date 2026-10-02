plugins {
    java
}

group = "com.soulswords"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

// Flat layout: all files sit in the repo root (easy to upload from a phone).
sourceSets {
    main {
        java {
            setSrcDirs(listOf("."))
            include("*.java")
        }
        resources {
            setSrcDirs(listOf("."))
            include("plugin.yml", "config.yml")
        }
    }
}
