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
    `java-library`
}

val gatlingVersion: String by project

dependencies {
    testImplementation("io.gatling.highcharts:gatling-charts-highcharts:${gatlingVersion}") {
        exclude(group = "io.gatling", module="gatling-jms")
        exclude(group = "io.gatling", module="gatling-jms-java")
        exclude(group = "io.gatling", module="gatling-mqtt")
        exclude(group = "io.gatling", module="gatling-mqtt-java")
        exclude(group = "io.gatling", module="gatling-jdbc")
        exclude(group = "io.gatling", module="gatling-jdbc-java")
        exclude(group = "io.gatling", module="gatling-redis")
        exclude(group = "io.gatling", module="gatling-redis-java")
        exclude(group = "io.gatling", module="gatling-graphite")
    }

    testImplementation(testFixtures(project(":common:util")))
    testImplementation(testFixtures(project(":launchers:junit")))

    testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.11.0")
    testRuntimeOnly(project(":system-tests:runtimes:file-transfer-provider"))
    testRuntimeOnly(project(":system-tests:runtimes:file-transfer-consumer"))
}

tasks.withType<Test> {
    val agent = rootDir.resolve("opentelemetry-javaagent.jar")
    if (agent.exists()) {
        jvmArgs(
                "-javaagent:${agent.absolutePath}",
                "-Dotel.traces.exporter=logging",
                "-Djava.util.logging.config.file=resources/logging.properties"
        );
    }
}

tasks.getByName<Test>("test") {
    testLogging {
        showStandardStreams = true
    }
}
