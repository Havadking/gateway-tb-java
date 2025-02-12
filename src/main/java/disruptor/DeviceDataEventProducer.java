package disruptor;

import com.lmax.disruptor.RingBuffer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.DeviceData;

/**
 * @program: gateway-netty
 * @description: Disruptor 事件生产者
 * @author: Havad
 * @create: 2025-02-08 16:32
 **/

@AllArgsConstructor
@Slf4j
public class DeviceDataEventProducer {
    public final RingBuffer<DeviceDataEvent> ringBuffer;

    /**
     * 处理设备数据事件。
     * <p>
     * 当接收到设备数据时调用此方法，将数据包装为事件并发布到环形缓冲区。
     *
     * @param data 要设置的数据
     * @param type 事件类型
     */
    public void onData(DeviceData data, DeviceDataEvent.Type type) {
        // 获取下一个可用的序列号
        long sequence = ringBuffer.next();
        try {
            // 获取该序列号对应的事件对象
            DeviceDataEvent event = ringBuffer.get(sequence);

            event.setValue(data);
            event.setType(type);
        } finally {
            // 发布事件
            ringBuffer.publish(sequence);
            log.info("生产了{},生产内容为{}", sequence, data);
        }
    }

}
