FROM eclipse-temurin:21-jre-alpine

COPY target/UserManagement.jar /opt/UserManagement/UserManagement.jar
EXPOSE 8080
WORKDIR /opt/UserManagement/
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-Xms512m", "-Xmx1g", "-jar", "UserManagement.jar"]