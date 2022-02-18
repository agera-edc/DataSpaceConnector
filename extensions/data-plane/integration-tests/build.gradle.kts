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

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${jupiterVersion}")
    testImplementation("io.rest-assured:rest-assured:4.5.0")
    testImplementation("org.assertj:assertj-core:3.22.0")
    testImplementation("org.awaitility:awaitility:4.1.1")
    testImplementation("com.github.javafaker:javafaker:1.0.2")
    testImplementation("org.mock-server:mockserver-netty:5.12.0:shaded")
    testImplementation("org.mock-server:mockserver-client-java:5.12.0:shaded")

    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))
//    testImplementation(project(":spi:core-spi"))
//    testImplementation(project(":extensions:data-plane:data-plane-http"))
    testImplementation(project(":extensions:data-plane:data-plane-spi"))

    testRuntimeOnly(project(":launchers:data-plane-server"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
