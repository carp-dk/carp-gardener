FROM gradle:7.2-jdk11 AS builder
COPY ./ /src
USER root
WORKDIR /src
RUN gradle clean shadowJar -x test


FROM openjdk:11
WORKDIR /app
COPY --from=builder /src/gardener.carp-implementation/build/libs/*.jar app.jar

RUN chown -R root:root /app
USER root

CMD echo "Starting CARP wearables service..." && exec java -jar app.jar
