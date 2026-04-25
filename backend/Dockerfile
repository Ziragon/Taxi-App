# Докерфайл строит образ из уже собранного jar-файла
# Запускается или в ci, или jar'ники уже были собраны локально
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
ARG JAR_PATH
COPY ${JAR_PATH} app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]