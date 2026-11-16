package com.health.healthserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 体检报告的 AI 解读（单独一张表存储，一份报告一条记录）。
 */
@Data
@TableName("exam_interpretation")
public class ExamInterpretation {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联的体检报告 id */
    @TableField("exam_id")
    private Integer examId;

    @TableField("user_id")
    private Integer userId;

    /** AI 解读文本（三段式：异常说明/生活建议/复查提醒） */
    private String content;

    /** 使用的模型标识 */
    private String model;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
