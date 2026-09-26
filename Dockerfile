FROM amazoncorretto:25

WORKDIR /app

COPY target/FrameWorkExtension-1.0-SNAPSHOT.jar app.jar
COPY src/main/resources/public public

ENV STATIC_PATH=public

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]