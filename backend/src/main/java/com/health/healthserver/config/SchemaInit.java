package com.health.healthserver.config;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Statement;

/**
 * 启动时自动建表（幂等）。
 * 用于新增的 ai_chat_message 表，避免在部署/本地手动执行建表 SQL，
 * 也避免在命令中暴露数据库密码。
 */
@Component
@Order(1)
public class SchemaInit implements CommandLineRunner {

    @Resource
    private DataSource dataSource;

    @Override
    public void run(String... args) {
        String sql = "CREATE TABLE IF NOT EXISTS ai_chat_message ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT NOT NULL, "
                + "role VARCHAR(20) NOT NULL, "
                + "content TEXT NOT NULL, "
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + "INDEX idx_ai_chat_user (user_id)) "
                + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        // 体检报告表（指标值 JSON 存储，AI 解读结果缓存）
        String examSql = "CREATE TABLE IF NOT EXISTS physical_exam ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT NOT NULL, "
                + "exam_date DATE NOT NULL, "
                + "hospital VARCHAR(200) DEFAULT '', "
                + "items_json TEXT, "
                + "ai_interpretation LONGTEXT, "
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "INDEX idx_physical_exam_user (user_id)) "
                + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        // 体检报告 AI 解读表（一份报告一条，按 exam_id 唯一关联）
        String interpSql = "CREATE TABLE IF NOT EXISTS exam_interpretation ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "exam_id INT NOT NULL, "
                + "user_id INT NOT NULL, "
                + "content LONGTEXT, "
                + "model VARCHAR(64) DEFAULT '', "
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "UNIQUE KEY uk_exam_interpretation_exam (exam_id), "
                + "INDEX idx_exam_interpretation_user (user_id)) "
                + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
            st.execute(examSql);
            st.execute(interpSql);
        } catch (Exception e) {
            // 建表失败不应阻断启动，仅打印日志
            e.printStackTrace();
        }
    }
}
