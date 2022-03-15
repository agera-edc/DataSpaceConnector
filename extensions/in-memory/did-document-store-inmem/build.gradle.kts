plugins {
    `java-library`
}

val jwtVersion: String by project


dependencies {
}

publishing {
    publications {
        create<MavenPublication>("did-document-store-memory") {
            artifactId = "did-document-store-memory"
            from(components["java"])
        }
    }
}
