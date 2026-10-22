package com.lethouse.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 任务调度服务启动类：批量任务编排、MQ 生产、任务状态回写。
 */
@SpringBootApplication(scanBasePackages = {"com.lethouse.task", "com.lethouse.common"})
public class TaskSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskSchedulerApplication.class, args);
    }
}
