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

val okHttpVersion: String by project
val storageBlobVersion: String by project;
val jodahFailsafeVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))
    implementation(project(":common:util"))
    implementation("com.azure.resourcemanager:azure-resourcemanager-datafactory:1.0.0-beta.12")
    implementation("com.azure:azure-identity:1.4.4")
    implementation("com.azure.resourcemanager:azure-resourcemanager:2.12.0")
    implementation("com.azure.resourcemanager:azure-resourcemanager-storage:2.12.0")
    implementation("com.azure:azure-security-keyvault-secrets:4.2.3")
    testImplementation("commons-io:commons-io:2.11.0")

    testImplementation(testFixtures(project(":extensions:azure:azure-test")))
    testImplementation(testFixtures(project(":common:util")))
}

publishing {
    publications {
        create<MavenPublication>("data-plane-azure-data-factory") {
            artifactId = "data-plane-azure-data-factory"
            from(components["java"])
        }
    }
}
