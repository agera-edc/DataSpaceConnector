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
 *       Microsoft Corporation - initial implementation
 *
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val kafkaClientsVersion: String by project

dependencies {
    implementation(project(":core"))

    implementation(project(":extensions:in-memory:assetindex-memory"))
    implementation(project(":extensions:in-memory:policy-store-memory"))
    implementation(project(":extensions:in-memory:transfer-store-memory"))
    implementation(project(":extensions:in-memory:negotiation-store-memory"))
    implementation(project(":extensions:in-memory:contractdefinition-store-memory"))

    implementation(project(":extensions:api:observability"))

    implementation(project(":extensions:filesystem:configuration-fs"))
    implementation(project(":extensions:iam:iam-mock"))

    implementation(project(":extensions:api:data-management"))

    implementation(project(":data-protocols:ids"))

    implementation(project(":core:transfer"))
    implementation(project(":extensions:data-plane-transfer:data-plane-transfer-client"))
    implementation(project(":extensions:data-plane-selector:selector-client"))
    implementation(project(":extensions:data-plane-selector:selector-core"))
    implementation(project(":extensions:data-plane-selector:selector-store"))
    implementation(project(":extensions:data-plane:data-plane-framework"))
    implementation(project(":extensions:data-plane:data-plane-kafka-transfer"))
    implementation(project(":extensions:data-plane:data-plane-http"))

    implementation("org.apache.kafka:kafka-clients:${kafkaClientsVersion}")
}

application {
    mainClass.set("org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("provider.jar")
}
