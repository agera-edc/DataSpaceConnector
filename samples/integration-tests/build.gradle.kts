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

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("io.rest-assured:rest-assured:4.5.0")
    testImplementation("org.assertj:assertj-core:3.22.0")
    testImplementation("org.awaitility:awaitility:4.1.1")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.28.0")

    testImplementation(testFixtures(project(":common:util")))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}