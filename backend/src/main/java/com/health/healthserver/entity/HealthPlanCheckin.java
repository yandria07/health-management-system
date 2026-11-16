package com.health.healthserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_plan_checkin")
public class HealthPlanCheckin {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;      // 关联用户ID（与 health_plan.user_id 一致）
    private Long planId;      // 关联计划ID
    private LocalDate checkinDate; // 打卡日期（每天一条）

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
