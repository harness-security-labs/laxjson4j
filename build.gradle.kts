plugins {
	`java-library`
	id("com.diffplug.spotless") version "8.4.0"
	id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "ai.traceable"
version = providers.environmentVariable("LAXJSON4J_VERSION").orNull?.takeIf { it.isNotBlank() }
	?: "0.1.0-SNAPSHOT"
description = "Lenient, high-performance JSON parser for Java 11+"

// The build JDK (toolchain) defaults to 17 but can be overridden per CI matrix entry
// via -PjavaVersion, so the suite is exercised on multiple JVMs (including 11).
val buildJavaVersion = (providers.gradleProperty("javaVersion").orNull ?: "17").toInt()

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(buildJavaVersion)
	}
}

// Target Java 11 bytecode + API for every source set so the published artifact runs
// on Java 11+ and the whole project (including tests) stays Java 11 compatible.
tasks.withType<JavaCompile>().configureEach {
	options.release.set(11)
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("ch.randelshofer:fastdoubleparser:1.0.1")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation(platform("org.junit:junit-bom:5.10.0"))
	testImplementation("org.junit.jupiter:junit-jupiter")

	testImplementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
	testImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
	testImplementation("com.fasterxml.jackson.core:jackson-annotations:2.18.2")
}

tasks.test {
	useJUnitPlatform()
}

tasks.javadoc {
	(options as StandardJavadocDocletOptions).apply {
		addStringOption("Xdoclint:none", "-quiet")  // don't fail the build on javadoc lint
		encoding = "UTF-8"
		charSet = "UTF-8"
		links("https://docs.oracle.com/en/java/javase/17/docs/api/")
}
}

spotless {
	format("misc") {
		// define the files to apply `misc` to
		target("*.gradle.kts", ".gitattributes", ".gitignore")

		trimTrailingWhitespace()
		leadingSpacesToTabs()
		endWithNewline()
	}
	java {
		target("src/**/*.java")
		importOrder()
		removeUnusedImports()
		cleanthat()
		googleJavaFormat()
		formatAnnotations()
	}
}

tasks.check {
	dependsOn(tasks.named("spotlessCheck"))
}

mavenPublishing {
	publishToMavenCentral(automaticRelease = true)
	signAllPublications()
	coordinates("ai.traceable", "laxjson4j", project.version.toString())
	pom {
		name.set("laxjson4j")
		description.set(project.description)
		url.set("https://github.com/traceableai/laxjson4j")
		inceptionYear.set("2026")
		licenses {
			license {
				name.set("Apache-2.0")
				url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
				distribution.set("repo")
			}
		}
		developers {
			developer {
				id.set("traceableai")
				name.set("Traceable AI")
				email.set("oss@traceable.ai")
				organization.set("Traceable AI")
				organizationUrl.set("https://www.traceable.ai")
			}
		}
		scm {
			url.set("https://github.com/traceableai/laxjson4j")
			connection.set("scm:git:git://github.com/traceableai/laxjson4j.git")
			developerConnection.set("scm:git:ssh://git@github.com/traceableai/laxjson4j.git")
		}
	}
}

tasks.withType<org.gradle.plugins.signing.Sign>().configureEach {
	onlyIf { project.hasProperty("releaseSigning") }
}
