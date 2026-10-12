plugins {
    id("java")
    id("biscuit")
}

version = providers.gradleProperty("mod_version").get()

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.isFork = true
}

biscuit.minecraft {
    version = "26.3"
}

repositories {
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    implementation("at.yawk.lz4:lz4-java:1.10.1")
}
