FROM eclipse-temurin:21-jre-alpine

COPY target/UserManagement.jar /opt/msTest/UserManagement/UserManagement.jar
EXPOSE 8080
WORKDIR /opt/msTest/UserManagement/
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-Xms512m", "-Xmx1g", "-Dspring.profiles.active=docker", "-jar", "UserManagement.jar"]