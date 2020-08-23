FROM adoptopenjdk/openjdk11:ubi

RUN mkdir /app
COPY build/libs/*.jar /app/

WORKDIR /app

CMD ["java", "-Dfile.encoding=UTF-8", "-Dlog4j.configurationFile=/app/data/log4j2.yml", "-Dspring.config.location=/app/data/application.yml", "-jar", "/app/vacation-bot-1.0-SNAPSHOT-all.jar"]