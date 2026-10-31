# Etapa de compilação
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
# Os testes de integração dependem de um PostgreSQL próprio; execute-os antes do deploy.
RUN mvn clean package -DskipTests -B

# Etapa de execução
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

# O Render injeta PORT em tempo de execução. A aplicação escuta 0.0.0.0.
EXPOSE 8080 10000

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60.0", "-XX:+UseSerialGC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
