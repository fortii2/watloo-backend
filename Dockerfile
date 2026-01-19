FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre AS final
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/data
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]