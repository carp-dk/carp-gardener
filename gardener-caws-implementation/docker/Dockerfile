FROM gradle:9.8.2-jdk21 AS builder
WORKDIR /src
COPY . .
RUN gradle :caws-implementation:shadowJar --no-daemon -x test


FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /src/gardener-caws-implementation/build/libs/*-fat.jar app.jar
EXPOSE 8444

CMD echo "Starting CARP wearables service..." && exec java -jar app.jar
