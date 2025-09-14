# Use an OpenJDK base image
FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the JAR file to the container
COPY target/homework-service-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8085

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
