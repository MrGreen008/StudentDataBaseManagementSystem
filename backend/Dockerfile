FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY . .
RUN mvn -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/backend/target/vishvaya-portal-0.1.0-SNAPSHOT.jar /app/application.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
