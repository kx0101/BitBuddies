plugins {
    java
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

application {
    // mainClass.set("com.kx0101.testClient.TestClient")
    mainClass.set("com.kx0101.testClientStreaming.TestClientStreaming")
}
