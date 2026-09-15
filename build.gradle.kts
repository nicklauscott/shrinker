plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.eclipsett"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	val sqliteVersion = "3.51.3.0"
	val exposed = "1.2.0"

	implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
	implementation("org.jetbrains.exposed:exposed-dao:$exposed")
	implementation("org.jetbrains.exposed:exposed-core:$exposed")
	implementation("org.jetbrains.exposed:exposed-jdbc:$exposed")

	implementation("software.amazon.awssdk:s3:2.20.90")
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
	implementation("org.springframework.boot:spring-boot-starter-mail")

	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")

	implementation("ch.qos.logback:logback-classic")

	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
