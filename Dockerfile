# ---------- Stage 1: build ----------
# Берём полноценный JDK (Ubuntu Jammy), чтобы запустить Gradle Wrapper
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Копируем только wrapper и build-скрипты — это ускорит кеширование слоёв
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY settings.gradle* build.gradle* gradle.properties* ./
RUN chmod +x gradlew

# Проверим, что wrapper скачался и JDK ок (и прогреем кеш)
RUN ./gradlew --version --no-daemon

# Теперь копируем исходники (и всё остальное, что нужно для сборки)
COPY . .

# Собираем приложение. Если есть тесты — чаще отключают на CI образе.
# Для Spring Boot: goal 'bootJar'. Для обычного JAR: поменяй на 'build' или свой таск.
RUN ./gradlew clean bootJar -x test --no-daemon

# ---------- Stage 2: runtime ----------
# Лёгкий JRE — для запуска. Можно distroless, но Temurin проще дебажить.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# (опционально) Нерутовый пользователь внутри контейнера
RUN useradd -m -u 10001 appuser
USER appuser

# Копируем единственный собранный jar из билдер-стейджа
# (если артефактов несколько — укажи точное имя файла)
COPY --from=build /app/build/libs/*.jar /app/app.jar

# Порт для веб-режима (если бот будет работать по webhook)
EXPOSE 8080

# Набор разумных JVM-флагов для контейнеров
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# Режим можно переключать переменной окружения MODE=worker|web
# (если у тебя в приложении такое предусмотрено)
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar ${MODE:+--mode=${MODE}}"]
