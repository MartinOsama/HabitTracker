FROM gradle:8.2.1-jdk17 AS build
WORKDIR /app
COPY . /app
RUN gradle clean shadowJar                       \
    --no-daemon                                  \
    -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/build/libs/*-all.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java","-jar","app.jar"]
