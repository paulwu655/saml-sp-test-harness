jar
```bash
mvn clean package
```

Dockerfile
```
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY saml-sp-test-harness.jar app.jar
VOLUME /data
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

```bash
docker compose --env-file harness.env up --build  
```
