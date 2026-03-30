FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
COPY document-service/pom.xml document-service/pom.xml
COPY collaboration-service/pom.xml collaboration-service/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml

RUN mvn -B -pl gateway-service -am dependency:go-offline

COPY . .
RUN mvn -B -pl gateway-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/gateway-service/target/gateway-service-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
