package com.health.healthserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.health.healthserver.dto.DashboardQueryDTO;
import com.health.healthserver.dto.HealthDataQueryDTO;
import com.health.healthserver.entity.HealthData;
import com.health.healthserver.response.R;
import com.health.healthserver.service.HealthDataService;
import com.health.healthserver.utils.UserContext;
import com.health.healthserver.vo.DashboardVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/health-data")
public class HealthDataController {

    @Resource
    private HealthDataService healthDataService;

    @PostMapping("/list")
    public R<PageInfo<HealthData>> list(@RequestBody HealthDataQueryDTO query) {
        Integer userId = UserContext.getUserId().intValue(); // 从 JWT 上下文中拿
        if (userId == null) return R.fail("未登录");

        LambdaQueryWrapper<HealthData> wrapper = new LambdaQueryWrapper<>();

        // 核心：强制只查当前用户的数据
        wrapper.eq(HealthData::getUserId, userId);

        // 日期范围查询
        if (query.getStartDate() != null && !query.getStartDate().isEmpty()) {
            wrapper.ge(HealthData::getRecordDate, query.getStartDate());
        }
        if (query.getEndDate() != null && !query.getEndDate().isEmpty()) {
            wrapper.le(HealthData::getRecordDate, query.getEndDate());
        }

        // 按记录日期倒序
        wrapper.orderByDesc(HealthData::getRecordDate);

        PageHelper.startPage(query.getPageNumber(), query.getPageSize());
        List<HealthData> list = healthDataService.list(wrapper);
        PageInfo<HealthData> pageInfo = new PageInfo<>(list);

        return R.data(pageInfo);
    }

    @PostMapping("/add")
    public R<?> add(@RequestBody HealthData data) {
        Integer userId = UserContext.getUserId().intValue();
        if (userId == null) return R.fail("未登录");

        data.setUserId(userId); // 强制绑定当前用户
        healthDataService.save(data);
        return R.success("添加成功");
    }

    @PostMapping("/update")
    public R<?> update(@RequestBody HealthData data) {
        Integer userId = UserContext.getUserId().intValue();
        if (userId == null) return R.fail("未登录");

        HealthData dbData = healthDataService.getById(data.getId());
        if (dbData == null) return R.fail("数据不存在");
        if (!dbData.getUserId().equals(userId)) return R.fail("无权操作");

        data.setUserId(userId);
        healthDataService.updateById(data);
        return R.success("更新成功");
    }

    @PostMapping("/del")
    public R<?> delete(@RequestParam Integer id) {
        Integer userId = UserContext.getUserId().intValue();
        if (userId == null) return R.fail("未登录");

        HealthData dbData = healthDataService.getById(id);
        if (dbData == null) return R.fail("数据不存在");
        if (!dbData.getUserId().equals(userId)) return R.fail("无权操作");

        healthDataService.removeById(id);
        return R.success("删除成功");
    }
    @PostMapping("/dashboard")
    public R<DashboardVO> dashboard(@RequestBody DashboardQueryDTO query) {
        Integer userId = UserContext.getUserId().intValue();
        if (userId == null) return R.fail("未登录");

        LambdaQueryWrapper<HealthData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthData::getUserId, userId);

        // 时间范围过滤
        LocalDate now = LocalDate.now();
        if ("7d".equals(query.getTimeRange())) {
            wrapper.ge(HealthData::getRecordDate, now.minusDays(6)); // 包含今天共7天
        } else if ("30d".equals(query.getTimeRange())) {
            wrapper.ge(HealthData::getRecordDate, now.minusDays(29)); // 包含今天共30天
        }
        // "all" 则不添加时间过滤

        // 按日期正序，方便前端图表按时间轴画线
        wrapper.orderByAsc(HealthData::getRecordDate);
        List<HealthData> list = healthDataService.list(wrapper);

        DashboardVO vo = new DashboardVO();
        vo.setTrendList(list);
        if (!list.isEmpty()) {
            vo.setLatestData(list.get(list.size() - 1)); // 最后一条即最新数据
        }
        return R.data(vo);
    }
}