# Java版本的ThingsBoard网关
## 使用框架和技术
- Netty: 负责设备TCP连接的管理
- Paho MQTT: 负责数据在网关和 ThingsBoard 之间的转换
- Disruptor: 负责在数据在 TCP 和 MQTT 之间的分发

## 整体架构
![image.png](https://raw.githubusercontent.com/Havadking/pictures/master/obsidian/20250217091613718.png)

- **协议选择器**：实现根据不同协议设备发送的首包数据解析，识别该设备的设备协议类型
- 数据流构建器：Netty的数据流处理器管理类。使用工厂设计模式，预先注册不同协议所需的全部处理器，根据设备类型选择其对应的数据流处理
    - 数据解析器：
    - 设备认证器：
    - 数据处理器：
- Disruptor 数据转发：
- MQTT 处理模块：
    - MQTT 信息构建器：
    - MQTT 信息解析器：
    - MQTT 发送器：
    - MQTT 解析器：
- TCP数据构建器：



## 参考文档
https://www.meng.me/posts/53523.html

https://github.com/mengDot/tb-java-gateway

https://www.meng.me/posts/31755.html

https://www.meng.me/posts/41934.html

http://www.ithingsboard.com/docs/iot-gateway/what-is-iot-gateway/

http://www.ithingsboard.com/docs/reference/mqtt-api/

https://geekdaxue.co/read/chaining@thingsboard/eh54gw
