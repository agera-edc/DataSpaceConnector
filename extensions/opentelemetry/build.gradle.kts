/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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
    `java-test-fixtures`
    `maven-publish`
}

val jupiterVersion: String by project
val armeriaVersion: String by project

dependencies {
    testFixturesImplementation(project(":spi"))
    testFixturesImplementation(testFixtures(project(":common:util")))
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:${jupiterVersion}")
    testFixturesImplementation("com.linecorp.armeria:armeria-grpc-protocol:${armeriaVersion}")
    testFixturesImplementation("com.linecorp.armeria:armeria-junit5:${armeriaVersion}")
    testFixturesImplementation("io.opentelemetry.proto:opentelemetry-proto:0.14.0-alpha")
}
