FROM alpine/git
WORKDIR /app
RUN git clone https://github.com/ikbened/VdekMockAdaptor.git

FROM maven:3.5-jdk-8-alpine
WORKDIR /app
COPY --from=0 /app/VdekMockAdaptor /app 
RUN mvn install 

FROM openjdk:8-jre-alpine
WORKDIR /app
COPY --from=1 /app/target/VdekMockAdaptor-1.0-SNAPSHOT.jar /app
EXPOSE 4444 
CMD ["java -jar VdekMockAdaptor-1.0-SNAPSHOT.jar"]