package gataway.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gataway.config.RedisConfig;
import gataway.util.LogUtils;
import redis.clients.jedis.JedisPooled;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @program: gateway-netty
 * @description:
 * @author: Havad
 * @create: 2025-02-17 17:03
 **/

public class TaskManager {
    /**
     * Jedis 实例，用于操作 Redis 数据库的连接池。
     */
    private final JedisPooled jedis = RedisConfig.getJedisPool();
    private final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * 定时任务执行器服务，使用单线程的线程池。
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 任务超时时间，以秒为单位。
     */
    private static final long TASK_TIMEOUT_SECONDS = 120;
    /**
     * 待处理任务键的前缀
     */
    private static final String PENDING_TASKS_KEY_PREFIX = "pending_tasks_set:";
    /**
     * 任务键的前缀。
     */
    private static final String TASK_KEY_PREFIX = "task:";
    /**
     * 上次发送任务的关键字前缀，用于存储设备键与上次发送的任务 ID 之间的映射关系
     */// 存储 deviceKey -> 上次发送的 taskId 的映射
    private static final String LAST_SENT_TASK_KEY_PREFIX = "last_sent_task:";

    private String getPendingTasksKey(String deviceKey) {
        return PENDING_TASKS_KEY_PREFIX + deviceKey;
    }

    private String getTaskKey(String taskId) {
        return TASK_KEY_PREFIX + taskId;
    }

    private String getLastSentTaskKey(String deviceKey) {
        return LAST_SENT_TASK_KEY_PREFIX + deviceKey;
    }

    /**
     * 添加来自 Thingsboard 的任务
     *
     * @param deviceKey 设备的唯一标识
     * @param task      要添加的任务对象
     */
    public void addTask(String deviceKey, Task task) {
        try {
            String taskJson = objectMapper.writeValueAsString(task);
            jedis.set(getTaskKey(task.getTaskId()), taskJson);
            // 使用 sadd 添加到 Set
            jedis.sadd(getPendingTasksKey(deviceKey), task.getTaskId());

            LogUtils.logBusiness("向设备{}的redis中写入了任务{}", deviceKey, task.getTaskId());
        } catch (JsonProcessingException e) {
            LogUtils.logError("Error serializing task", e);
        }
    }


    /**
     * 获取下一个待处理任务。
     *
     * @param deviceKey 设备标识
     * @return 下一个待处理任务对象，如果不存在则返回null
     */
    @SuppressWarnings({"checkstyle:ReturnCount", "checkstyle:MagicNumber"})
    public Task getNextTaskToProcess(String deviceKey) {
        // 从 Set 中随机弹出一个 task ID(由于每个设备只有一个在处理的任务，所以随机弹出和按顺序弹出没有区别)
        String taskId = jedis.spop(getPendingTasksKey(deviceKey));
        if (taskId != null) {
            try {
                LogUtils.logBusiness("取出task:{}", taskId);
                String taskJson = jedis.get(getTaskKey(taskId));
                if (taskJson != null) {
                    Task task = objectMapper.readValue(taskJson, Task.class);
                    task.setStatus(Task.TaskStatus.SENT);
                    task.setTimestamp(System.currentTimeMillis());
                    String sentTaskJson = objectMapper.writeValueAsString(task);
                    jedis.set(getLastSentTaskKey(deviceKey), sentTaskJson);


                    scheduler.schedule(() -> {
                        String lastSentTaskJson = jedis.get(getLastSentTaskKey(deviceKey));
                        if (lastSentTaskJson != null) {
                            try {
                                Task lastSentTask = objectMapper.readValue(lastSentTaskJson, Task.class);
                                // Check if still SENT and timed out
                                if (lastSentTask.getStatus() == Task.TaskStatus.SENT
                                        && System.currentTimeMillis() - lastSentTask.getTimestamp()
                                        > TASK_TIMEOUT_SECONDS * 1000) {
                                    markTaskFailed(lastSentTask.getTaskId());
                                }
                            } catch (JsonProcessingException e) {
                                LogUtils.logError("Error deserializing last sent task", e);
                            }
                        }
                    }, TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    return task;
                } else {
                    LogUtils.logError("Task data not found for ID: {}", new Throwable(), taskId);
                    jedis.lrem(getPendingTasksKey(deviceKey), 0, taskId);
                    return null;
                }
            } catch (JsonProcessingException e) {
                LogUtils.logError("Error deserializing task", e);
                removeTask(taskId);
                return null;
            }
        }
        return null;
    }

    /**
     * 获取指定设备最后一次发送的任务。
     *
     * @param deviceKey 设备标识键
     * @return 最后一次发送的任务，如果不存在或解析失败则返回null
     */
    public Task getLastSentTask(String deviceKey) {
        String taskJson = jedis.get(getLastSentTaskKey(deviceKey));
        if (taskJson != null) {
            try {
                LogUtils.logBusiness("获取上一次发送的任务{}", deviceKey);
                return objectMapper.readValue(taskJson, Task.class);
            } catch (JsonProcessingException e) {
                LogUtils.logError("Error deserializing last sent task", e);
                return null;
            }
        }
        return null;
    }


    /**
     * 标记任务已发送
     *
     * @param taskId 任务ID
     */
    public void markTaskSent(String taskId) {
        updateTaskStatus(taskId, Task.TaskStatus.SENT, null);
        LogUtils.logBusiness("任务{}被设定为发送", taskId);
    }

    /**
     * 标记任务成功完成
     *
     * @param taskId 任务标识符
     */
    public void markTaskSuccess(String taskId) {
        updateTaskStatus(taskId, Task.TaskStatus.SUCCESS, null);
        removeLastSentTask(taskId);
        removeTask(taskId);
        LogUtils.logBusiness("Task {} completed successfully", taskId);
    }

    /**
     * 标记任务失败
     *
     * @param taskId 任务ID
     * @see #updateTaskStatus(String, Task.TaskStatus, String)
     * @see #removeTask(String)
     */
    public void markTaskFailed(String taskId) {
        updateTaskStatus(taskId, Task.TaskStatus.FAILED, "Task timed out");
        removeLastSentTask(taskId);
        removeTask(taskId);
        LogUtils.logBusiness("Task {} failed: {}", taskId, "Task timed out");
    }

    private void removeLastSentTask(String taskId) {
        // Iterate through all devices and remove the last sent task if it matches the given taskId
        String taskJson = jedis.get(getTaskKey(taskId));
        if (taskJson != null) {
            try {
                Task task = objectMapper.readValue(taskJson, Task.class);
                String deviceKey = task.getDeviceKey(); // 直接从 Task 对象获取 deviceKey
                jedis.del(getLastSentTaskKey(deviceKey)); // 删除 last_sent_task:{deviceKey}
                LogUtils.logBusiness("移除了设备{}的上一次发送的任务{}", deviceKey, taskId);
            } catch (JsonProcessingException e) {
                LogUtils.logError("Error deserializing task in removeLastSentTask", e);
            }
        }
    }


    /**
     * 移除指定任务
     *
     * @param taskId 要移除的任务ID
     */
    private void removeTask(String taskId) {
        LogUtils.logBusiness("移除任务{}", taskId);
        // 删除任务详情
        jedis.del(getTaskKey(taskId));

        // 从 Set 中移除 (只需要操作一次)
        Set<String> keys = jedis.keys(PENDING_TASKS_KEY_PREFIX + "*");
        for (String key : keys) {
            jedis.srem(key, taskId);
        }
    }

    /**
     * 更新任务状态。
     *
     * @param taskId        任务ID
     * @param status        要更新的任务状态
     * @param failureReason 失败原因，若任务状态为失败，此参数为失败原因；否则可以为空
     */
    private void updateTaskStatus(String taskId, Task.TaskStatus status, String failureReason) {
        String taskJson = jedis.get(getTaskKey(taskId));
        if (taskJson != null) {
            try {
                Task task = objectMapper.readValue(taskJson, Task.class);
                task.setStatus(status);
                if (failureReason != null) {
                    task.setFailureReason(failureReason);
                }
                jedis.set(getTaskKey(taskId), objectMapper.writeValueAsString(task));
            } catch (JsonProcessingException e) {
                LogUtils.logError("Error updating task status", e);
            }
        }
    }

    /**
     * 关闭调度器
     * <p>
     * 此方法将立即关闭调度器，停止所有正在执行的任务。
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
