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

# preferIPv4Stack is the candidate fix for this branch. The same image reaches
# Neon in 185ms on Render and times out after 10s on Brimble, and one thing that
# produces exactly that - a connection that hangs with no server response at all
# - is the JVM choosing an IPv6 address for an endpoint that only answers on
# IPv4. Forcing IPv4 costs nothing if the theory is wrong.
#
# MaxRAMPercentage is 50 rather than 70 because this app's non-heap use is
# large (metaspace alone measures ~84MB), and on a small container 70% for the
# heap leaves too little for the rest, which ends in a silent OOM kill.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=50 -XX:MaxMetaspaceSize=160m -Djava.net.preferIPv4Stack=true"

# Report what the container's network can actually do, on this branch only.
ENV NETWORK_DIAGNOSTICS=true

# Expose the port the app runs on (app binds ${PORT:8080})
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
