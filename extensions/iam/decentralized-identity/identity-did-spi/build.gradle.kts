plugins {
    `java-library`
}

dependencies {
    api(project(":spi"))
    api(project(":common:json-web-crypto-spi"))
}

publishing {
    publications {
        create<MavenPublication>("identity-did-spi") {
            artifactId = "identity-did-spi"
            from(components["java"])
        }
    }
}
