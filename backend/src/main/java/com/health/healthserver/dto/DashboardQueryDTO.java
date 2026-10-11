package com.health.healthserver.dto;

import lombok.Data;

@Data
public class DashboardQueryDTO {
    // "7d" 近7天, "30d" 近30天, "all" 全部历史
    private String timeRange = "7d";
}