package com.health.healthserver.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_plan")
public class HealthPlan {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId; // 关联用户ID
    private String planName;
    private String planType;
    private String description;
    private LocalDate startDate; // 日期类型，和前端 el-date-picker 匹配
    private LocalDate endDate;
    private String remindTime;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}