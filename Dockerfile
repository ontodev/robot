FROM maven:3-jdk-8-alpine

RUN adduser -D robot
RUN mkdir -p /usr/src/app
RUN chown robot /usr/src/app

COPY pom.xml /usr/src/app
COPY robot-command /usr/src/app/robot-command
COPY robot-core /usr/src/app/robot-core
COPY robot-maven-plugin /usr/src/app/robot-maven-plugin
COPY bin/robot /usr/local/bin/
RUN chown -R robot:robot /usr/src/app

USER robot
WORKDIR /usr/src/app
RUN mvn install -DskipTests

USER root
RUN cp bin/robot.jar /usr/local/bin
USER robot
ENTRYPOINT ["robot"]
