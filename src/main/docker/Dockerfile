FROM anapsix/alpine-java
VOLUME /tmp
ADD cgatesql-0.0.1.jar app.jar
EXPOSE 9005
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
