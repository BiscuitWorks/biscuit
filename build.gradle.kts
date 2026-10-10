plugins {
    id("java")
    id("biscuit")
}

version = providers.gradleProperty("mod_version").get()

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.isFork = true
}
