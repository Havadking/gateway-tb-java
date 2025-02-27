FROM reg.xxt.cn/library/java:8u111-jre
VOLUME /tmp
ADD target/gateway-netty.jar /app/app.jar
EXPOSE 5566 12000 12001 12002
CMD ["java", "-jar", "app.jar"]

