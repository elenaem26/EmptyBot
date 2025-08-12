FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY settings.gradle* build.gradle* gradle.properties* ./
RUN chmod +x gradlew

RUN ./gradlew --version --no-daemon

COPY . .

RUN ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

# Набор разумных JVM-флагов для контейнеров
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar ${MODE:+--mode=${MODE}}"]
