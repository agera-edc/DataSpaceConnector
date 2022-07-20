plugins {
    `java-library`
    `java-test-fixtures`
}

val datafaker: String by project

dependencies {
    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))
    implementation(project(":extensions:iam:decentralized-identity:identity-did-crypto"))

    testImplementation(testFixtures(project(":extensions:iam:decentralized-identity:identity-common-test")))
    testImplementation(project(":extensions:junit"))
    testImplementation("net.datafaker:datafaker:${datafaker}")
}

publishing {
    publications {
        create<MavenPublication>("identity-did-service") {
            artifactId = "identity-did-service"
            from(components["java"])
        }
    }
}
