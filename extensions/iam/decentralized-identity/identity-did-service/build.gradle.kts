plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))
    implementation(project(":common:token-validation-lib"))
    implementation(project(":common:token-generation-lib"))
    implementation(project(":common:json-web-crypto-lib"))

    testImplementation(testFixtures(project(":extensions:iam:decentralized-identity:identity-common-test")))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-service") {
            artifactId = "identity-did-service"
            from(components["java"])
        }
    }
}
