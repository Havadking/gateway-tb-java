package cn.xxt.gatewaynetty.netty.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description:
 * @author: Havad
 * @create: 2025-02-17 16:58
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    public enum TaskStatus {
        /**
         * 年龄
         */
        PENDING,
        /**
         * 发送状态码
         */
        SENT,
        /**
         * 操作成功状态标识
         */
        SUCCESS,
        /**
         * 失败
         */
        FAILED
    }

    /**
     * 任务ID
     */
    private String taskId;
    /**
     * 设备号
     */
    private String deviceKey;
    /**
     * 人员列表，其中每个人员的信息以键值对形式存储在Map中。
     */
    private List<Map<String, Object>> personList;
    /**
     * 任务状态
     */
    private TaskStatus status;
    /**
     * 失败原因
     */
    private String failureReason;
    /**
     * 时间戳
     */
    private long timestamp;

    public Task(String taskId, String deviceKey, List<Map<String, Object>> personList) {
        this.taskId = taskId;
        this.deviceKey = deviceKey;
        this.personList = personList;
        this.status = TaskStatus.PENDING;
    }
}
