package com.health.healthserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.healthserver.common.ExamItemCatalog;
import com.health.healthserver.entity.ExamInterpretation;
import com.health.healthserver.entity.PhysicalExam;
import com.health.healthserver.response.R;
import com.health.healthserver.service.AiService;
import com.health.healthserver.service.ExamInterpretationService;
import com.health.healthserver.service.PhysicalExamService;
import com.health.healthserver.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Tag(name = "体检报告")
@RestController
@RequestMapping("/physical-exam")
public class PhysicalExamController {

    @Resource private PhysicalExamService examService;
    @Resource private ExamInterpretationService interpService;
    @Resource private AiService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 获取当前用户 id，未登录返回 null */
    private Integer currentUserId() {
        Long uid = UserContext.getUserId();
        return uid == null ? null : uid.intValue();
    }

    /** 指标目录（分类 + 22 项指标及参考范围） */
    @Operation(summary = "获取体检指标目录")
    @PostMapping("/catalog")
    public R<?> catalog() {
        return R.data(ExamItemCatalog.getCatalog());
    }

    /** 当前用户的体检报告列表（按日期倒序） */
    @Operation(summary = "体检报告列表")
    @PostMapping("/list")
    public R<?> list() {
        Integer userId = currentUserId();
        if (userId == null) return R.fail("未登录");
        LambdaQueryWrapper<PhysicalExam> w = new LambdaQueryWrapper<>();
        w.eq(PhysicalExam::getUserId, userId).orderByDesc(PhysicalExam::getExamDate);
        return R.data(examService.list(w));
    }

    /** 报告详情：把 itemsJson 展开为带 normal/high/low 状态的明细 */
    @Operation(summary = "体检报告详情")
    @PostMapping("/detail")
    public R<?> detail(@RequestParam Integer id) {
        Integer userId = currentUserId();
        if (userId == null) return R.fail("未登录");
        PhysicalExam exam = examService.getById(id);
        if (exam == null) return R.fail("报告不存在");
        if (!exam.getUserId().equals(userId)) return R.fail("无权查看");

        Map<String, Object> values = parseItems(exam.getItemsJson());
        // 从解读表读取（兼容旧数据：表中无记录时回退读实体上的历史字段）
        String interpretation = findInterpretation(exam.getId());
        if (interpretation == null) interpretation = exam.getAiInterpretation();

        Map<String, Object> data = new HashMap<>();
        data.put("exam", exam);
        data.put("details", ExamItemCatalog.buildDetail(values));
        data.put("interpretation", interpretation);
        return R.data(data);
    }

    /** 新增或更新报告（body 含 id 则更新，否则新增） */
    @Operation(summary = "保存体检报告")
    @PostMapping("/save")
    public R<?> save(@RequestBody Map<String, Object> body) {
        Integer userId = currentUserId();
        if (userId == null) return R.fail("未登录");

        Integer id = body.get("id") == null ? null : Integer.valueOf(body.get("id").toString());
        String examDateStr = body.get("examDate") == null ? null : body.get("examDate").toString();
        String hospital = body.get("hospital") == null ? "" : body.get("hospital").toString().trim();
        @SuppressWarnings("unchecked")
        Map<String, Object> values = body.get("items") instanceof Map
                ? (Map<String, Object>) body.get("items") : new HashMap<>();

        if (examDateStr == null || examDateStr.isEmpty()) return R.fail("请选择体检日期");
        if (values.isEmpty()) return R.fail("请至少录入一项指标");

        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            return R.fail("指标数据格式错误");
        }

        PhysicalExam exam;
        if (id != null) {
            exam = examService.getById(id);
            if (exam == null) return R.fail("报告不存在");
            if (!exam.getUserId().equals(userId)) return R.fail("无权操作");
        } else {
            exam = new PhysicalExam();
            exam.setUserId(userId);
        }
        exam.setExamDate(LocalDate.parse(examDateStr));
        exam.setHospital(hospital);
        exam.setItemsJson(itemsJson);
        // 指标变更后，旧的 AI 解读失效，清空缓存（新表 + 实体字段）
        // 注意：updateById 默认忽略 null 字段，这里用空串确保兼容字段被真正清空
        exam.setAiInterpretation("");
        examService.saveOrUpdate(exam);
        interpService.remove(new LambdaQueryWrapper<ExamInterpretation>()
                .eq(ExamInterpretation::getExamId, exam.getId()));
        return R.data(exam.getId());
    }

    /** 删除报告（只能删自己的） */
    @Operation(summary = "删除体检报告")
    @PostMapping("/del")
    public R<?> delete(@RequestParam Integer id) {
        Integer userId = currentUserId();
        if (userId == null) return R.fail("未登录");
        PhysicalExam exam = examService.getById(id);
        if (exam == null) return R.fail("报告不存在");
        if (!exam.getUserId().equals(userId)) return R.fail("无权操作");
        examService.removeById(id);
        interpService.remove(new LambdaQueryWrapper<ExamInterpretation>()
                .eq(ExamInterpretation::getExamId, id));
        return R.success("删除成功");
    }

    /**
     * AI 解读：把该报告的异常项批量传给智谱，输出通俗解读并缓存
     */
    @Operation(summary = "AI 解读体检报告")
    @PostMapping("/interpret")
    public R<?> interpret(@RequestParam Integer id) {
        Integer userId = currentUserId();
        if (userId == null) return R.fail("未登录");
        PhysicalExam exam = examService.getById(id);
        if (exam == null) return R.fail("报告不存在");
        if (!exam.getUserId().equals(userId)) return R.fail("无权操作");

        // 命中解读表缓存则直接返回
        String cached = findInterpretation(exam.getId());
        if (cached != null && !cached.isEmpty()) return R.data(cached);
        // 兼容旧数据：实体字段有缓存则迁移到新表后返回
        if (exam.getAiInterpretation() != null && !exam.getAiInterpretation().isEmpty()) {
            saveInterpretation(exam, exam.getAiInterpretation());
            return R.data(exam.getAiInterpretation());
        }

        Map<String, Object> values = parseItems(exam.getItemsJson());
        String abnormalText = ExamItemCatalog.buildAbnormalText(values);

        try {
            String interpretation = aiService.interpretExam(abnormalText);
            // 写入独立解读表（一份报告一条）
            saveInterpretation(exam, interpretation);
            // 同步实体字段，保持向后兼容
            exam.setAiInterpretation(interpretation);
            examService.updateById(exam);
            return R.data(interpretation);
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("AI 解读失败：" + e.getMessage());
        }
    }

    /** 从解读表查询某报告的解读文本 */
    private String findInterpretation(Integer examId) {
        LambdaQueryWrapper<ExamInterpretation> w = new LambdaQueryWrapper<>();
        w.eq(ExamInterpretation::getExamId, examId).last("LIMIT 1");
        ExamInterpretation rec = interpService.getOne(w);
        return rec == null ? null : rec.getContent();
    }

    /** 写入/更新解读表：一份报告一条记录 */
    private void saveInterpretation(PhysicalExam exam, String content) {
        LambdaQueryWrapper<ExamInterpretation> w = new LambdaQueryWrapper<>();
        w.eq(ExamInterpretation::getExamId, exam.getId()).last("LIMIT 1");
        ExamInterpretation rec = interpService.getOne(w);
        if (rec == null) {
            rec = new ExamInterpretation();
            rec.setExamId(exam.getId());
            rec.setUserId(exam.getUserId());
        }
        rec.setContent(content);
        rec.setModel("glm-4-flash");
        interpService.saveOrUpdate(rec);
    }

    /** 解析 itemsJson 为 Map */
    private Map<String, Object> parseItems(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
