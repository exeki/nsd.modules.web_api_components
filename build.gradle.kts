plugins {
    id("groovy")
    id("maven-publish")
    id("nsd_sdk") version "1.4.1"
}

group = "ru.kazantsev.nsd.modules"
version = "2.1.0"

tasks.javadoc{
    options.encoding = "UTF-8"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/exeki/nsd.modules.web_api_components")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

sdk {
    addRepositories()
    addAppDependencies()
    addDevDependencies()
}

dependencies {
    implementation ("org.codehaus.groovy:groovy-all:3.0.19")
}

