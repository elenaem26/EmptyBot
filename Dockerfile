# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY settings.gradle* build.gradle* gradle.properties* ./
RUN chmod +x gradlew
RUN ./gradlew --version --no-daemon

COPY . .
RUN ./gradlew clean bootJar -x test --no-daemon


# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr

RUN apt-get update && DEBIAN_FRONTEND=noninteractive \
    apt-get install -y --no-install-recommends \
      tesseract-ocr libtesseract-dev libleptonica-dev \
      curl ca-certificates imagemagick \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p ${TESSDATA_PREFIX}/tessdata && \
    curl -L -o ${TESSDATA_PREFIX}/tessdata/kat.traineddata \
      https://github.com/tesseract-ocr/tessdata_best/raw/main/kat.traineddata && \
    curl -L -o ${TESSDATA_PREFIX}/tessdata/eng.traineddata \
      https://github.com/tesseract-ocr/tessdata_best/raw/main/eng.traineddata && \
    curl -L -o ${TESSDATA_PREFIX}/tessdata/osd.traineddata \
      https://github.com/tesseract-ocr/tessdata_best/raw/main/osd.traineddata

# 3) Копируем приложение
COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

# Набор разумных JVM-флагов для контейнеров
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar ${MODE:+--mode=${MODE}}"]
