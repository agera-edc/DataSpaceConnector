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
val assertj: String by project
val awaitility: String by project
val gatlingVersion: String by project
val faker: String by project

dependencies {
    testImplementation("io.gatling:gatling-http-java:${gatlingVersion}")
    testImplementation("io.gatling.highcharts:gatling-charts-highcharts:${gatlingVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testImplementation("org.assertj:assertj-core:${assertj}")
    testImplementation("org.awaitility:awaitility:${awaitility}")
    testImplementation("com.github.javafaker:javafaker:${faker}")

    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))

    testRuntimeOnly(project(":system-tests:runtimes:file-transfer-provider"))
    testRuntimeOnly(project(":system-tests:runtimes:file-transfer-consumer"))
}

tasks.getByName<Test>("test") {
    testLogging {
        showStandardStreams = true
    }
}
