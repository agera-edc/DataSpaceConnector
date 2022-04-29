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

plugins {
    java
}

val jupiterVersion: String by project
val restAssured: String by project
val awaitility: String by project
val assertj: String by project
val httpMockServer: String by project
val kafkaClientsVersion: String by project

dependencies {
    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))

    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("org.awaitility:awaitility:${awaitility}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")

    testImplementation("org.mock-server:mockserver-netty:${httpMockServer}:shaded")
    testImplementation("org.mock-server:mockserver-client-java:${httpMockServer}:shaded")
    testImplementation(project(":extensions:data-plane:data-plane-kafka-models"))
    testImplementation("org.apache.kafka:kafka-clients:${kafkaClientsVersion}")

    testCompileOnly(project(":system-tests:kafka-transfer-test:provider"))
    testCompileOnly(project(":system-tests:kafka-transfer-test:consumer"))
}
