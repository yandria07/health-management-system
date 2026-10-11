package com.health.healthserver.dto; // 👈 注意：必须在 dto 文件夹下，包名要对应

import lombok.Data;

@Data
public class HealthPlanQueryDTO {

    // ===== 分页参数 =====
    // 当前页码，默认第 1 页
    private Integer pageNumber = 1;

    // 每页条数，默认 10 条
    private Integer pageSize = 10;

    // ===== 查询条件 =====
    // 核心：当前登录用户的ID（用来过滤只查该用户的数据）
    private Integer user_id;

    // 计划名称（用于模糊搜索）
    private String plan_name;

    // 计划类型（用于精确筛选，如“运动”）
    private String plan_type;

    // 状态（用于精确筛选，如“进行中”）
    private String status;
}