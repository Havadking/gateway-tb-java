# 使用官方的 Maven 镜像作为构建环境
FROM maven:3.8.4-openjdk-17 AS build
# 设置工作目录
WORKDIR /app
# 复制项目的 pom.xml 文件
COPY pom.xml .
# 下载项目依赖
RUN mvn dependency:go-offline -B
# 复制项目源代码
COPY src ./src
# 构建项目
RUN mvn clean package -DskipTests

# 使用官方的 OpenJDK 镜像作为运行环境
FROM openjdk:17-jdk-slim
# 设置工作目录
WORKDIR /app
# 从构建环境中复制生成的 JAR 文件
COPY --from=build /app/target/gateway-netty.jar ./gateway-netty.jar
# 暴露端口
EXPOSE 5566 12000 12001 12002
# 启动应用程序
CMD ["java", "-jar", "gateway-netty.jar"]