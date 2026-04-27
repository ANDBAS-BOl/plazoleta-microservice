FROM eclipse-temurin:17-jre
WORKDIR /app
ARG JAR_FILE=build/libs/plazoleta-microservice-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
