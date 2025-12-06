FROM eclipse-temurin:21-jre-alpine

COPY target/SalesManagement.jar /opt/msTest/SalesManagement/SalesManagement.jar
EXPOSE 8081
WORKDIR /opt/msTest/SalesManagement/
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-Xms512m", "-Xmx1g", "-Dspring.profiles.active=docker", "-jar", "SalesManagement.jar"]