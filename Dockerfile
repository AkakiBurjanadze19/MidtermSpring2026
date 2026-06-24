# Builder stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn clean install -DskipTests -B
COPY src ./src
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/uno-cli-1.0.0.jar ./app.jar
COPY --from=builder /app/target/lib ./lib
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD []