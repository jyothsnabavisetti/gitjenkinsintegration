FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
# Copy the built jar (match any jar produced by Maven)
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]