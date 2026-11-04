FROM maven:3.10.0-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests
FROM tomcat:10.1.61-jre25-temurin-noble
COPY --from=build /app/target/FlashSale.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 10000
CMD ["sh", "-c", "sed -i 's/port=\"8080\"/port=\"'\"${PORT:-10000}\"'\"/' \"$CATALINA_HOME/conf/server.xml\" && catalina.sh run"]
