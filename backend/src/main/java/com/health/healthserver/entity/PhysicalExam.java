package com.health.healthserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体检报告：一份报告一条记录，指标值以 JSON 存 itemsJson。
 */
@Data
@TableName("physical_exam")
public class PhysicalExam {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    /** 体检日期 */
    @TableField("exam_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate examDate;

    /** 体检机构（可选） */
    private String hospital;

    /** 各指标值 JSON：{"height":170,"bmi":22.5,...} */
    @TableField("items_json")
    private String itemsJson;

    /** AI 解读结果（缓存），空表示尚未解读 */
    @TableField("ai_interpretation")
    private String aiInterpretation;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
