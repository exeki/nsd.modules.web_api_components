plugins {
    id("groovy")
    id("maven-publish")
}

group = "ru.kazantsev.nsd.sdk"
version = "1.0.0"

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

dependencies {
    implementation ("ru.kazantsev.nsd.sdk:upper_level_classes:1.0.0")
    implementation ("ru.kazantsev.nsd.sdk:global_variables:1.0.0")
    implementation ("org.codehaus.groovy:groovy-all:3.0.19")
    implementation ("org.springframework:spring-web:5.3.30")
    implementation ("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation ("javax.servlet:javax.servlet-api:4.0.1")
}

