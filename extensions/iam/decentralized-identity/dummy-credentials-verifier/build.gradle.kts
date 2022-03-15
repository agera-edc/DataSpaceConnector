plugins {
    `java-library`
}

dependencies {


    // this is required for the JcaPEMKeyConverter, which we use to restore keys from PEM files

}
publishing {
    publications {
        create<MavenPublication>("dummy-credentials-verifier") {
            artifactId = "dummy-credentials-verifier"
            from(components["java"])
        }
    }
}
