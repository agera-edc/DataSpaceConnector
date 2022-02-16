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
val storageBlobVersion: String by project;
val jodahFailsafeVersion: String by project

plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:data-plane:data-plane-spi"))
    implementation(project(":common:util"))
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("com.azure:azure-storage-blob:${storageBlobVersion}")
    implementation("net.jodah:failsafe:${jodahFailsafeVersion}")
}

publishing {
    publications {
        create<MavenPublication>("data-plane-azure-storage") {
            artifactId = "data-plane-azure-storage"
            from(components["java"])
        }
    }
}
