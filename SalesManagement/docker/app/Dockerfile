FROM eclipse-temurin:21-jre-alpine

COPY target/SalesManagement.jar /opt/UserManagement/SalesManagement.jar
EXPOSE 8080
WORKDIR /opt/msTest/SalesManagement/
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-Xms512m", "-Xmx1g", "-jar", "SalesManagement.jar"]