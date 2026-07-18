# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:21-jdk-alpine AS build

# Set the working directory
WORKDIR /app

# Copy the file from your host to your current location
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make the wrapper executable
RUN chmod +x mvnw

# Download dependencies (offline build)
RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src src

# Package the application
RUN ./mvnw package -DskipTests

# Start with a clean image
FROM eclipse-temurin:21-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the jar file from the build stage (version-agnostic)
COPY --from=build /app/target/roamSafe-*.jar app.jar

# Default to the production profile; bound the heap to the container's memory
# so it behaves on small (free-tier) VMs. Override JAVA_OPTS/PORT as needed.
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"

# Expose the port the app runs on (app binds ${PORT:8080})
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
