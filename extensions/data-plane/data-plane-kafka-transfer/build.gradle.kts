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
}

val kafkaClientsVersion: String by project

dependencies {
    implementation(project(":common:util"))
    implementation(project(":extensions:data-plane:data-plane-kafka-models"))
    implementation(project(":extensions:data-plane:data-plane-spi"))
    implementation("org.apache.kafka:kafka-clients:${kafkaClientsVersion}")
}

publishing {
    publications {
        create<MavenPublication>("data-plane-kafka-transfer") {
            artifactId = "data-plane-kafka-transfer"
            from(components["java"])
        }
    }
}
