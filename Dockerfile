FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -q -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/target/Verteiler-*.jar verteiler.jar

RUN apk add --no-cache curl

VOLUME ["/app/data", "/app/logs", "/app/config"]

ENV VERTEILER_HOST=0.0.0.0
ENV VERTEILER_PORT=2987

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD curl -fsS --max-time 5 http://127.0.0.1:${VERTEILER_PORT}/api/health || exit 1

EXPOSE 2987

ENTRYPOINT ["java", "-jar", "verteiler.jar"]