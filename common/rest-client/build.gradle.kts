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

// https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin
// https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/java.md


var jakarta_annotation_version: String = "1.3.5"

plugins {
    java
    id("org.openapi.generator") version "5.4.0"
}

apply(plugin = "org.openapi.generator")

tasks.withType(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class.java) {
    generatorName.value("java")
    inputSpec.value(file("$rootDir/resources/openapi/openapi.yaml").getAbsolutePath())
    validateSpec.value(false)
    configOptions.set(
        mapOf(
            "invokerPackage" to "org.eclipse.dataspaceconnector.client",
            "apiPackage" to "org.eclipse.dataspaceconnector.client.api",
            "modelPackage" to "org.eclipse.dataspaceconnector.client.models",
        )
    )
}

// compileJava.dependsOn tasks.openApiGenerate
val compileJava: JavaCompile by tasks
val openApiGenerate: org.openapitools.generator.gradle.plugin.tasks.GenerateTask by tasks
compileJava.apply {
    dependsOn(openApiGenerate)
}
sourceSets {
    main {
        java {
            srcDirs(
                "$buildDir/generate-resources/main/src/main/java"
            )
        }
    }
}

dependencies {
    // dependencies copied from build/generate-resources/main/build.gradle
    implementation("io.swagger:swagger-annotations:1.5.24")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("io.gsonfire:gson-fire:1.8.4")
    implementation("org.openapitools:jackson-databind-nullable:0.2.1")
    implementation("org.apache.commons:commons-lang3:3.10")
    implementation("org.threeten:threetenbp:1.4.3")
    implementation("jakarta.annotation:jakarta.annotation-api:$jakarta_annotation_version")
}

publishing {
    publications {
        create<MavenPublication>("rest-client") {
            artifactId = "rest-client"
            from(components["java"])
        }
    }
}
