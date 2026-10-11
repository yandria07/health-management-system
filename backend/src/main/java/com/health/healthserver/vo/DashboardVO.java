package com.health.healthserver.vo;   // 👈 必须是这个

import com.health.healthserver.entity.HealthData;
import lombok.Data;
import java.util.List;

@Data
public class DashboardVO {
    private HealthData latestData;
    private List<HealthData> trendList;
}