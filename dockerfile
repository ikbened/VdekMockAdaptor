FROM openjdk:8-jre-slim
RUN mkdir /app
WORKDIR /app
COPY target/VdekMockAdaptor-1.0-SNAPSHOT.jar /app
COPY startup.sh /app
RUN chmod 777 startup.sh
EXPOSE 4444
ENTRYPOINT ["/app/startup.sh"]