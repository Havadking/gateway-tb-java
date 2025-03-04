FROM reg.xxt.cn/library/java:8u111-jre
VOLUME /tmp
ADD target/gateway-netty.jar /app/app.jar
EXPOSE 8080
CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]

