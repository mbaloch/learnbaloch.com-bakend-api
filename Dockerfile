FROM openjdk:24-ea-17-jdk-slim
COPY target/translate-api-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p src/main/resources
COPY src/main/resources/learnbalochi-firebase-adminsdk-fbsvc-50dbb19d8d.json \
    src/main/resources/learnbalochi-firebase-adminsdk-fbsvc-50dbb19d8d.json
EXPOSE 9090
ENTRYPOINT ["java","-jar","app.jar"]