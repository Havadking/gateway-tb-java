## 基础MQTT API

### 设备连接API

通过ThingsBoard设备已连接到网关需要发布以下消息：

```plaintext
1. Topic: v1/gateway/connect
2. Message: {"device":"Device A"}
```

**Device A** 代表你的设备名称。
收到后ThingsBoard将查找或创建具有指定名称的设备。
ThingsBoard还将向该网关发布有关特定设备的新属性性更新和RPC命令的消息。

---

### 设备断开API

为了通知ThingsBoard设备已与网关断开连接，需要发布以下消息：

```plaintext
1. Topic: v1/gateway/disconnect
2. Message: {"device":"Device A"}
```

**Device A** 代表你的设备名称。
一旦收到ThingsBoard将不再将此特定设备的更新发布到此网关。

---



### 设备遥测数据API

为了将设备遥测数据发布到ThingsBoard服务器节点，请将PUBLISH消息发送到以下主题：

```plaintext
Topic: v1/gateway/telemetry
```

#### 消息结构

```json
{
  "Device A": [
    {
      "ts": 1483228800000,
      "values": {
        "temperature": 42,
        "humidity": 80
      }
    },
    {
      "ts": 1483228801000,
      "values": {
        "temperature": 43,
        "humidity": 82
      }
    }
  ],
  "Device B": [
    {
      "ts": 1483228800000,
      "values": {
        "temperature": 42,
        "humidity": 80
      }
    }
  ]
}
```

#### 说明

- **Device A** 和 **Device B** 代表设备名称。
- **temperature** 和 **humidity** 为遥测数据键。
- **ts** 为Unix时间戳（毫秒级）。

---


## RPC API

### 服务器端RPC

为了从服务器订阅RPC命令，请将SUBSCRIBE消息发送到以下主题：

```plaintext
v1/gateway/rpc
```

#### 使用以下带有单个命令的消息格式：

```json
{
  "device": "Device A",
  "data": {
    "id": $request_id,
    "method": "toggle_gpio",
    "params": {
      "pin": 1
    }
  }
}
```

#### 设备处理完命令后，网关可以使用以下格式将命令发送回：

```json
{
  "device": "Device A",
  "id": $request_id,
  "data": {
    "success": true
  }
}
```

#### 参数说明

- **`$request_id`**：你的整数请求标识符。
- **`Device A`**：你的设备名称。
- **`method`**：RPC方法名。

