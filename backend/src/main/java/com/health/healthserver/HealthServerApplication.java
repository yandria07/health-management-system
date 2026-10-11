package com.health.healthserver; // 👈 必须是这个路径，和你现在的文件夹路径一致

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // 👈 核心注解，开启 Spring Boot 自动配置
@MapperScan("com.health.healthserver.mapper")
public class HealthServerApplication {

    public static void main(String[] args) {
        // 👈 启动 Spring Boot 应用
        SpringApplication.run(HealthServerApplication.class, args);
        System.out.println("健康管理系统后端启动成功！");
    }
}