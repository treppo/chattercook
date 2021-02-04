FROM openjdk:8-alpine

COPY target/uberjar/chattercook.jar /chattercook/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/chattercook/app.jar"]
