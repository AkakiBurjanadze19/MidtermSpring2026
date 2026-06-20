# Use an official JDK runtime as a parent image
FROM eclipse-temurin:25-jdk

# Set the working directory
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Run the application
CMD ["java", "-jar", "target/uno-cli-1.0.0.jar"]