FROM eclipse-temurin:17-jdk-alpine AS compilacao
WORKDIR /compilacao
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S sirs && adduser -S -G sirs sirs
WORKDIR /aplicacao
COPY --from=compilacao --chown=sirs:sirs /compilacao/target/sirs-api-*.jar aplicacao.jar
USER sirs
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "aplicacao.jar"]
