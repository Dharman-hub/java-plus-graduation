FROM amazoncorretto:21-alpine
COPY target/*.jar stat.jar
ENTRYPOINT ["java","-jar","/stat.jar"]