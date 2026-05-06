FROM maven:3.9.10-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /app/target/kal-backend-app.jar /app/kal-backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/kal-backend.jar"]
