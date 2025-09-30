import org.gradle.kotlin.dsl.implementation

plugins {
    java
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // 스프링부트 스타터
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation ("org.springframework.boot:spring-boot-starter-websocket")
    implementation ("org.springframework.boot:spring-boot-starter-webflux")


    // 스프링부트 추가 기능
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    // 롬복
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 개발 도구
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // DB
    runtimeOnly("com.h2database:h2")

    // QueryDSL
    implementation("io.github.openfeign.querydsl:querydsl-jpa:7.0")
    annotationProcessor("io.github.openfeign.querydsl:querydsl-apt:7.0:jpa")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.github.codemonstur:embedded-redis:1.4.2")
    implementation("net.datafaker:datafaker:2.1.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
    filter {
        includeTestsMatching("com.backend.domain.member.controller.ApiV1MemberControllerTest")
    }
}

// QueryDSL 설정 - 생성된 Q클래스들이 저장될 디렉토리 설정
val querydslDir = "src/main/generated"

sourceSets {
    main {
        java.srcDirs(querydslDir)
    }
}

tasks.withType<JavaCompile> {
    options.generatedSourceOutputDirectory.set(file(querydslDir))
}

tasks.named("clean") {
    doLast {
        delete(file(querydslDir))
    }
}