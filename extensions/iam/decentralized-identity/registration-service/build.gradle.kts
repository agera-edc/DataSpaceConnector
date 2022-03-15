plugins {
    `java-library`
}

val jwtVersion: String by project
val rsApi: String by project
val okHttpVersion: String by project

dependencies {

    // third party
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}")
}

publishing {
    publications {
        create<MavenPublication>("registration-service") {
            artifactId = "registration-service"
            from(components["java"])
        }
    }
}
