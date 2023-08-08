FROM eclipse-temurin:17-jdk as build

WORKDIR /workspace/app

ARG ID_LOG_VERSION=1.0.0-SNAPSHOT
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
COPY libs libs
COPY urls.env.json .
COPY configurations configurations

RUN ./mvnw -q install:install-file -Dfile=libs/commons-1.0.0-SNAPSHOT.jar -DgroupId=ee.ria.java.commons -DartifactId=commons -Dversion=1.0.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
RUN ./mvnw -q install:install-file -Dfile=libs/id-log-1.0.0-SNAPSHOT.jar -DgroupId=ee.ria.commons -DartifactId=id-log -Dversion=1.0.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true

ENTRYPOINT ["./mvnw", "spring-boot:run"]
