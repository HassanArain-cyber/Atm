FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY ATMWebServer.java .
RUN javac ATMWebServer.java

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/*.class ./
COPY public/ ./public/
ENV PORT=8080
EXPOSE 8080
CMD ["java","ATMWebServer"]
