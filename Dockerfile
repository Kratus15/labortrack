# Build the spring boot application with Java 21
FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven configurations first so Docker can cache dependencies
COPY pom.xml .

RUN mvn -B -DskipTests dependency:go-offline

# copy the application source code
COPY src ./src

# Build the executable spring boot JAR
RUN mvn -B -DskipTests clean package


# run the application with a smaller Java 21 runtime image
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]