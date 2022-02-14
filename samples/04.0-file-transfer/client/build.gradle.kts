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
    id("application")
}

dependencies {
    implementation(project(":spi"))

    implementation("io.rest-assured:rest-assured:4.5.0")
    implementation("org.assertj:assertj-core:3.22.0")
    implementation("org.awaitility:awaitility:4.1.1")
}

application {
    mainClass.set("org.eclipse.dataspaceconnector.samples.PerformFileTransfer")
}
