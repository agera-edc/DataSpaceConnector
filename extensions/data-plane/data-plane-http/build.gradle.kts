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
 *       Microsoft Corporation - initial API and implementation
 *
 */

val okHttpVersion: String by project
val jodahFailsafeVersion: String by project
val rsApi: String by project

plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))
    implementation(project(":common:util"))
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("net.jodah:failsafe:${jodahFailsafeVersion}")
    testFixturesImplementation("com.github.javafaker:javafaker:1.0.2")
    testFixturesImplementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
}

publishing {
    publications {
        create<MavenPublication>("data-plane-http") {
            artifactId = "data-plane-http"
            from(components["java"])
        }
    }
}
