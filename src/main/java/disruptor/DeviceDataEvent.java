package disruptor;

import lombok.Getter;
import lombok.Setter;

/**
 * @program: gateway-netty
 * @description: Disruptor 事件
 * @author: Havad
 * @create: 2025-02-08 17:03
 **/

@Getter
@Setter
public class DeviceDataEvent {
    private String value;
    private Type type;

    public enum Type {
        TO_TB,  //发送给TB
        TO_DEVICE //发送给设备
    }

    public void clear() {
        value = null;
    }
}
