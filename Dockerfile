# # FROM openjdk:24-ea-17-jdk-slim
# # COPY target/translate-api-0.0.1-SNAPSHOT.jar app.jar
# # RUN mkdir -p src/main/resources
# # COPY src/main/resources/learnbalochi-firebase-adminsdk-fbsvc-50dbb19d8d.json \
# #     src/main/resources/learnbalochi-firebase-adminsdk-fbsvc-50dbb19d8d.json
# # EXPOSE 9090
# # ENTRYPOINT ["java","-jar","app.jar"]

# FROM maven:3.9.10-eclipse-temurin-24-noble AS builder
# WORKDIR /app
# COPY pom.xml .
# RUN mvn dependency:go-offline -B
# COPY src/ src/
# RUN mkdir -p src/main/resources
# COPY src/main/resources/learnbalochi-prod-firebase-adminsdk-fbsvc-learnbaluchi.json \
#     src/main/resources/learnbalochi-prod-firebase-adminsdk-fbsvc-learnbaluchi.json
# RUN mvn package -DskipTests -B

# FROM eclipse-temurin:24-jre
# WORKDIR /service
# COPY --from=builder /app/target/*.jar app.jar
# COPY src/main/resources/learnbalochi-prod-firebase-adminsdk-fbsvc-learnbaluchi.json .
# EXPOSE 9090
# ENTRYPOINT ["java","-jar","app.jar"]


# Stage 1: Build the application using Maven
FROM maven:3.9.10-eclipse-temurin-24-noble AS builder
WORKDIR /app
COPY pom.xml .
# Download dependencies first to leverage Docker layer caching
RUN mvn dependency:go-offline -B
COPY src/ src/
# The 'mvn package' command will bundle the service account key from src/main/resources into the JAR
RUN mvn package -DskipTests -B

# Stage 2: Create the final, smaller image
FROM eclipse-temurin:24-jre
WORKDIR /service
# Copy only the built JAR from the builder stage.
# The service account key is already inside this JAR.
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java","-jar","app.jar"]
