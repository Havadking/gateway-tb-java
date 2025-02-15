# Java版本的ThingsBoard网关
## 使用框架和技术
- Netty: 负责设备TCP连接的管理
- Paho MQTT: 负责数据在网关和 ThingsBoard 之间的转换
- Disruptor: 负责在数据在 TCP 和 MQTT 之间的分发

## 参考文档
https://www.meng.me/posts/53523.html

https://github.com/mengDot/tb-java-gateway

https://www.meng.me/posts/31755.html

https://www.meng.me/posts/41934.html

http://www.ithingsboard.com/docs/iot-gateway/what-is-iot-gateway/

http://www.ithingsboard.com/docs/reference/mqtt-api/

https://geekdaxue.co/read/chaining@thingsboard/eh54gw
