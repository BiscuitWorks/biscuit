plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("biscuit") {
            id = "biscuit"
            implementationClass = "biscuit.BiscuitPlugin"
        }
    }
}
