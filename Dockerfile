# Stage 1: Build the application using Maven
FROM maven:3-openjdk-11-slim AS build

# Create a working directory and set permissions
RUN useradd -m robot
RUN mkdir -p /usr/src/app
RUN chown robot /usr/src/app

WORKDIR /usr/src/app

# Copy the POM and source code to the working directory
COPY pom.xml /usr/src/app
COPY robot-command /usr/src/app/robot-command
COPY robot-core /usr/src/app/robot-core
COPY robot-maven-plugin /usr/src/app/robot-maven-plugin
COPY robot-mock-plugin /usr/src/app/robot-mock-plugin

# Change ownership to robot user
RUN chown -R robot:robot /usr/src/app

# Use the robot user to run Maven
USER robot

# Run the Maven build, skipping tests to speed up the process
RUN mvn install -DskipTests

# Stage 2: Create a smaller runtime container
FROM openjdk:11-jre-slim AS runtime

# Create a non-root user and set up a working directory
RUN useradd -m robot
RUN mkdir -p /usr/src/app/bin
RUN chown robot /usr/src/app/bin

# Copy the compiled JAR file from the build stage to the runtime stage
COPY --from=build /usr/src/app/bin/robot.jar /usr/src/app/bin/robot.jar

# Set robot as the user
USER robot

# Set the entrypoint to run the robot.jar
ENTRYPOINT ["java", "-jar", "/usr/src/app/bin/robot.jar"]
