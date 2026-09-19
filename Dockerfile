FROM eclipse-temurin:21-jre-noble

# Create an unprivileged account inside the image.
RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid 10001 \
       --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app

# Package the exact executable JAR we already built and tested.
COPY --chown=10001:10001 target/release-tracker-0.1.0-SNAPSHOT.jar /app/app.jar

# Listen on the container's network interfaces.
ENV SERVER_ADDRESS=0.0.0.0

# Run the application as this user, not root.
USER 10001:10001

# Document the application port; this does not publish it to the Mac.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
