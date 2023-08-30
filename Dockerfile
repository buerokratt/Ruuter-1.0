FROM eclipse-temurin:11 as build

WORKDIR /workspace/app

ARG ID_LOG_VERSION=1.0.0-SNAPSHOT
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
COPY libs libs
COPY urls.env.json .
COPY configurations configurations
RUN apt update && apt install nano

RUN ./mvnw -q install:install-file -Dfile=libs/commons-1.0.0-SNAPSHOT.jar -DgroupId=ee.ria.java.commons -DartifactId=commons -Dversion=1.0.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
RUN ./mvnw -q install:install-file -Dfile=libs/id-log-1.0.0-SNAPSHOT.jar -DgroupId=ee.ria.commons -DartifactId=id-log -Dversion=1.0.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true


RUN keytool -genkeypair -alias jwtsign -keyalg RSA -keysize 2048 -keystore "keystore.p12" -dname "CN=, OU=, O=, L=, ST=, C=" -storepass changeit -validity 3650

ENTRYPOINT ["./mvnw", "spring-boot:run"]
