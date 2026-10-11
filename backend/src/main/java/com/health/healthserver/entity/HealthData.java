package com.health.healthserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_data")
public class HealthData {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId; // 后端会强制设置，前端不需要传

    @TableField("record_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;

    private BigDecimal weight;
    private BigDecimal height;
    private BigDecimal bmi;

    @TableField("systolic_pressure")
    private Integer systolicPressure;

    @TableField("diastolic_pressure")
    private Integer diastolicPressure;

    @TableField("heart_rate")
    private Integer heartRate;

    private Integer steps;

    @TableField("sleep_hours")
    private BigDecimal sleepHours;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}