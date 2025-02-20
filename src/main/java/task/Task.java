package task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description:
 * @author: Havad
 * @create: 2025-02-17 16:58
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    public enum TaskStatus {
        PENDING,
        SENT,
        SUCCESS,
        FAILED
    }
    private String taskId;
    private String deviceKey;
    private List<Map<String, Object>> personList;
    private TaskStatus status;
    private String failureReason;
    private long timestamp;

    public Task(String taskId, String deviceKey, List<Map<String, Object>> personList) {
        this.taskId = taskId;
        this.deviceKey = deviceKey;
        this.personList = personList;
        this.status = TaskStatus.PENDING;
    }
}
