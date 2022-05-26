plugins {
    `java-library`
}

val nimbusVersion: String by project
dependencies {
    api(project(":common:json-web-crypto-spi"))
    implementation("org.bouncycastle:bcpkix-jdk15on:1.56")
}

publishing {
    publications {
        create<MavenPublication>("json-web-crypto") {
            artifactId = "json-web-crypto"
            from(components["java"])
        }
    }
}
