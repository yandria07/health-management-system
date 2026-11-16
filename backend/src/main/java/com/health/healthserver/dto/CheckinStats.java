package com.health.healthserver.dto;

import lombok.Data;

import java.util.List;

@Data
public class CheckinStats {
    private Integer totalCount;     // 累计打卡天数
    private Integer streak;         // 当前连续打卡天数
    private Integer weekCount;      // 近 7 天打卡次数
    private Boolean checkedToday;   // 今天是否已打卡
    private List<Boolean> last7;    // 近 7 天打卡情况（index 0=6天前 ... 6=今天）
}
