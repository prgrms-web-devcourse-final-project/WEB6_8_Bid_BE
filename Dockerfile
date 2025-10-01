# Runtime stage only
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG PG_TOSS_CLIENT_KEY
ARG PG_TOSS_SECRET_KEY

ENV PG_TOSS_CLIENT_KEY=${PG_TOSS_CLIENT_KEY}
ENV PG_TOSS_SECRET_KEY=${PG_TOSS_SECRET_KEY}

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy pre-built jar from local build
COPY build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

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
