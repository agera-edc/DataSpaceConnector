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
 *       Microsoft Corporation - initial test implementation for sample
 *
 */

plugins {
    `java-library`
}

val restAssured: String by project
val awaitility: String by project


dependencies {
    testImplementation(testFixtures(project(":launchers:junit")))
    testImplementation(testFixtures(project(":common:util")))

    testImplementation("io.rest-assured:rest-assured:${restAssured}")
    testImplementation("org.awaitility:awaitility:${awaitility}")

    //testCompileOnly(project(":samples:04.0-file-transfer:consumer"))
    //testCompileOnly(project(":samples:04.0-file-transfer:provider"))
    testRuntimeOnly(project(":samples:04.0-file-transfer:consumer"))
    testRuntimeOnly(project(":samples:04.0-file-transfer:provider"))

}
