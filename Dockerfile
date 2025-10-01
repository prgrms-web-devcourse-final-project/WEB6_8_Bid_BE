# STEP 1: Build Stage (빌드 환경)
#
FROM eclipse-temurin:21-jdk-alpine AS build

# Gradle 사용 시 필요한 도구 설치
RUN apk add --no-cache bash

WORKDIR /app

# 소스 코드 전체를 빌드 환경에 복사 (소스 코드 변경 시 이 Layer의 캐시가 깨집니다)
# COPY 명령 이전에 git log 등으로 파일 변경을 확인하는 것이 일반적이지만,
# Dockerfile 내에서는 이 COPY가 캐시를 깨는 주요 트리거입니다.
COPY . .

# Clean Build 실행 (SecurityConfig 변경 사항 포함)
# 항상 깨끗하게 빌드하여 최신 JAR 파일을 생성
RUN ./gradlew clean bootJar

#
# STEP 2: Runtime Stage (실행 환경)
#
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 환경 변수는 그대로 유지
ARG PG_TOSS_CLIENT_KEY
ARG PG_TOSS_SECRET_KEY
ENV PG_TOSS_CLIENT_KEY=${PG_TOSS_CLIENT_KEY}
ENV PG_TOSS_SECRET_KEY=${PG_TOSS_SECRET_KEY}

# Build Stage에서 생성된 JAR 파일만 복사
COPY --from=build /app/build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

# Verify JAR contents (디버깅용 검증 단계)
RUN jar tf app.jar | grep SecurityConfig || echo "WARNING: SecurityConfig not found in JAR"

# Add non-root user and change ownership
RUN addgroup -S spring && adduser -S spring -G spring && \
    chown spring:spring /app/app.jar

USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]