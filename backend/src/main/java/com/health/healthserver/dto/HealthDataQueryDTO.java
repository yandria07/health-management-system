package com.health.healthserver.dto;

import lombok.Data;

@Data
public class HealthDataQueryDTO {
    // 分页参数
    private Integer pageNumber = 1;
    private Integer pageSize = 10;

    // 筛选条件（日期范围）
    private String startDate;
    private String endDate;
}