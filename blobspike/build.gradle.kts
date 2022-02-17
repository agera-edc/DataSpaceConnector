/*
 *  Copyright (c) 2021 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

val infoModelVersion: String by project
val rsApi: String by project

plugins {
    `java-library`
}


dependencies {

    implementation("org.assertj:assertj-core:3.21.0")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    implementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    implementation("org.junit.jupiter:junit-jupiter-api:5.8.2")

    implementation("com.azure:azure-storage-blob:12.14.2")
}



