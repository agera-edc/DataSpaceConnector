plugins {
    `java-library`
}

val nimbusVersion: String by project

dependencies {
    api(project(":extensions:iam:decentralized-identity:identity-did-spi"))

    implementation("com.nimbusds:nimbus-jose-jwt:${nimbusVersion}")
}

publishing {
    publications {
        create<MavenPublication>("identity-did-crypto") {
            artifactId = "identity-did-crypto"
            from(components["java"])
        }
    }
}
