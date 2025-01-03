package mqtt;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @program: gateway-netty
 * @description:
 * @author: Havad
 * @create: 2025-01-03 09:58
 **/

public class ExecutorServiceExample {
    public static void main(String[] args) {
        // 创建一个固定大小的线程池，大小为 2
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // 提交任务
        executorService.submit(() -> {
            System.out.println("Task 1 is running...");
            try {
                Thread.sleep(1000); // 模拟任务耗时
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Task 1 is completed.");
        });

        executorService.submit(() -> {
            System.out.println("Task 2 is running...");
            try {
                Thread.sleep(500); // 模拟任务耗时
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Task 2 is completed.");
        });

        // 关闭线程池（任务完成后优雅退出）
        executorService.shutdown();
        System.out.println("Executor Service is shutting down.");
    }
}
