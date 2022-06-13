/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

val infoModelVersion: String by project
val jerseyVersion: String by project
val okHttpVersion: String by project
val rsApi: String by project
val restAssured: String by project

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

dependencies {
    api(project(":spi:contract-spi"))
    api(project(":spi:policy-spi"))
    api(project(":spi:transaction-spi"))
    implementation(project(":common:util"))
    implementation(project(":core:policy:policy-evaluator"))
    implementation(project(":extensions:api:api-core"))
    implementation(project(":extensions:api:data-management:api-configuration"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")

    testImplementation(project(":extensions:http"))

    testImplementation(project(":extensions:transaction:transaction-local"))

    testImplementation(project(":extensions:junit"))
    testImplementation("io.rest-assured:rest-assured:${restAssured}")
}

publishing {
    publications {
        create<MavenPublication>("policydefinition-api") {
            artifactId = "policydefinition-api"
            from(components["java"])
        }
    }
}
