package netty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeviceMessage {
    private String type;
    private String content;

    public DeviceMessage() {}

    public DeviceMessage(String type, String content) {
        this.type = type;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // 将对象转换为字节数组
    public byte[] toBytes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsBytes(this);
    }

    // 从字节数组解析对象
    public static DeviceMessage fromBytes(byte[] bytes) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(bytes, DeviceMessage.class);
    }

    @Override
    public String toString() {
        return "DeviceMessage{" +
                "type='" + type + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
