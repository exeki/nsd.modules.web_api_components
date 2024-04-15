plugins {
    id("groovy")
    id("maven-publish")
    id("nsd_sdk") version "1.3"
}

group = "ru.kazantsev.nsd.modules"
version = "1.0.1"

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
            pom {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
            }
        }
    }
    repositories {
        mavenLocal()
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

