package com.health.healthserver.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Component
public class DatabaseInitializer {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @PostConstruct
    public void init() {
        try {
            // 从 JDBC URL 中提取服务器地址和端口（去掉库名部分）
            String serverUrl = jdbcUrl;
            int dbStart = serverUrl.lastIndexOf('/');
            if (dbStart > -1 && dbStart < serverUrl.length() - 1) {
                // 提取 ? 之前的库名
                String afterServer = serverUrl.substring(dbStart + 1);
                int paramStart = afterServer.indexOf('?');
                String dbName = paramStart > -1 ? afterServer.substring(0, paramStart) : afterServer;
                // 如果库名不是 defaultdb，才创建
                if (!dbName.equals("defaultdb") && !dbName.isEmpty()) {
                    String connectUrl = serverUrl.substring(0, dbStart + 1) + "?" +
                            (paramStart > -1 ? afterServer.substring(paramStart + 1) : "");
                    // 尝试创建数据库
                    try (Connection conn = DriverManager.getConnection(connectUrl, username, password);
                         Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("数据库初始化跳过（可能已存在）: " + e.getMessage());
        }
    }
}
