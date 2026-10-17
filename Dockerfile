FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package 

FROM eclipse-temurin:17-jre-alpine-3.24
RUN apk upgrade --no-cache

WORKDIR /app
RUN addgroup -S -g 1001 app && adduser -S -u 1001 -G app app

COPY --from=build /workspace/target/fee-planner.jar app.jar
ENV PORT=8080 JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

USER app
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

