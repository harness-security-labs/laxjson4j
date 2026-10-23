plugins {
    `java`
    id("me.champeau.jmh") version "0.7.3"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    implementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("org.hjson:hjson:3.1.0")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.56")
    implementation("com.google.code.gson:gson:2.10.1")
}

jmh {
    if (project.hasProperty("jmh.includes")) {
        includes.set(listOf(project.property("jmh.includes") as String))
    }
    if (project.hasProperty("jmh.resultFormat")) {
        resultFormat.set(project.property("jmh.resultFormat") as String)
    }
    if (project.hasProperty("jmh.resultsFile")) {
        resultsFile.set(project.file(project.property("jmh.resultsFile") as String))
    }
    if (project.hasProperty("jmh.fork")) {
        fork.set((project.property("jmh.fork") as String).toInt())
    }
    if (project.hasProperty("jmh.warmupIterations")) {
        warmupIterations.set((project.property("jmh.warmupIterations") as String).toInt())
    }
    if (project.hasProperty("jmh.iterations")) {
        iterations.set((project.property("jmh.iterations") as String).toInt())
    }
    if (project.hasProperty("jmh.profilers")) {
        profilers.set(listOf(project.property("jmh.profilers") as String))
    }
}
