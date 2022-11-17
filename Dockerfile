FROM adoptopenjdk/openjdk11:jdk-11.0.5_10-alpine
EXPOSE 8088
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} resource-service.jar
ENTRYPOINT ["java","-jar","/resource-service.jar"]