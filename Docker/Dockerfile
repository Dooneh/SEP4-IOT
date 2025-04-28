# Base Image
FROM openjdk:21-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the project files (use a specific folder if needed)
COPY . /app/

# Rebuild the project 
#RUN ./mvnw clean install -U

# Expose the port for the application
EXPOSE 8080

# Run the JAR file (make sure the path is correct)
CMD ["java", "-jar", "/app/target/iot-1.0-SNAPSHOT.jar"]