/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

// REST client using OpenAPI Generator. See:
// https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin
// https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/java.md

plugins {
    java
    id("org.openapi.generator") version "5.4.0"
}

// Configure OpenAPI Generator
tasks.withType(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class.java) {
    generatorName.value("java")
    inputSpec.value(file("$rootDir/resources/openapi/openapi.yaml").absolutePath)
    validateSpec.value(false)
    configOptions.set(
        mapOf(
            "library" to "native",
            "dateLibrary" to "legacy",
            "useRuntimeException" to "true",
            "invokerPackage" to "org.eclipse.dataspaceconnector.client",
            "apiPackage" to "org.eclipse.dataspaceconnector.client.api",
            "modelPackage" to "org.eclipse.dataspaceconnector.client.models",
        )
    )
}

// Ensure compileJava depends on openApiGenerate
val compileJava: JavaCompile by tasks
val openApiGenerate: org.openapitools.generator.gradle.plugin.tasks.GenerateTask by tasks
compileJava.apply {
    dependsOn(openApiGenerate)
}

// Add generated sources
sourceSets {
    main {
        java {
            srcDirs(
                "$buildDir/generate-resources/main/src/main/java"
            )
        }
    }
}

// Add dependencies copied from build/generate-resources/main/build.gradle
var swagger_annotations_version = "1.5.22"
var jackson_version = "2.10.4"
var jakarta_annotation_version = "1.3.5"
var junit_version = "4.13.1"
var threetenbp_version = "2.9.10"
dependencies {
    implementation("io.swagger:swagger-annotations:$swagger_annotations_version")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.fasterxml.jackson.core:jackson-core:$jackson_version")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jackson_version")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
    implementation("org.openapitools:jackson-databind-nullable:0.2.1")
    implementation("jakarta.annotation:jakarta.annotation-api:$jakarta_annotation_version")
    implementation("com.github.joschi.jackson:jackson-datatype-threetenbp:$threetenbp_version")

}

// Generated Maven package
publishing {
    publications {
        create<MavenPublication>("rest-client") {
            artifactId = "rest-client"
            from(components["java"])
        }
    }
}
