## 设备对接thingsboard平台

### 基础配置

1. 服务器地址：192.168.5.121
2. 端口号：1883
3. 用户名和密码
    1. 用户名为thingsboard平台对应设备的设备凭证，可以通过thingsboard平台获取，或者通过姚延乐的接口进行自定义修改和获取
    2. 密码为空
       ![[CleanShot 2024-07-11 at 16.43.29.png]]

### 信息接收与发送

#### 设备接收TB消息并返回

1. 设备端mqtt连接成功后，需要订阅topic`v1/devices/me/rpc/request/+`。tb在接受到信息后，会发送到该topic上，设备可以接受到如下格式的信息。
   ![[CleanShot 2024-07-11 at 16.35.09 1.png]]
   设备需要的数据为data中的内容，但因为thingsboard的局限性以及iot平台的记录需求，所以设备接收到的为一个完整的信息，需要设备端进行解析。
2. 设备收到数据以后，需要返回给thingsboard平台证明设备收到了当前的数据。设备端需要向`v1/devices/me/rpc/response/{id}`这个topic发送json格式的信息。消息中应包含是否接受成功，以及接受到的taskId。
   **其中topic中的id值跟收到信息的id值必须对应。**
   一个例子如下：
   ![[CleanShot 2024-07-11 at 16.49.58.png]]

#### 设备心跳上报

设备的心跳以遥测的形式进行上报，thingsboard平台接受到心跳数据后进行后续处理，不需要回复设备。
发送的topic：`v1/devices/me/telemetry`
建议上传的心跳形式为：

``` json
{
	"heartbeat": 
	{
		"deviceKey":"865246060452326",
		"time":"1720687943694",
		"version":"1.0.02"
	}
}
```

