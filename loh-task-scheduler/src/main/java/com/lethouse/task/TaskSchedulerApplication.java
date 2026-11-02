package com.lethouse.task;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 任务调度服务启动类：批量任务编排、MQ 生产、任务状态回写。
 */
@SpringBootApplication(scanBasePackages = {"com.lethouse.task", "com.lethouse.common"})
@EnableScheduling
@MapperScan("com.lethouse.task.mapper")
public class TaskSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskSchedulerApplication.class, args);
    }
}
