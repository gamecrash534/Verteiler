FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -q -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/target/Verteiler-*.jar app.jar

VOLUME ["/app/data", "/app/logs"]

EXPOSE 2987

ENTRYPOINT ["java", "-jar", "app.jar"]