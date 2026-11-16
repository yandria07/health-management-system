package com.health.healthserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.healthserver.entity.AiAdvice;
import com.health.healthserver.entity.AiChatMessage;
import com.health.healthserver.entity.HealthData;
import com.health.healthserver.response.R;
import com.health.healthserver.service.AiAdviceService;
import com.health.healthserver.service.AiChatMessageService;
import com.health.healthserver.service.AiService;
import com.health.healthserver.service.HealthDataService;
import com.health.healthserver.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "AI健康建议")
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource private AiService aiService;
    @Resource private HealthDataService healthDataService;
    @Resource private AiAdviceService aiAdviceService;   // 👈 新增
    @Resource private AiChatMessageService chatService;  // 👈 对话消息服务

    /**
     * 生成建议 + 立即保存
     * 前端可传入 mentalHealth（心理健康测评摘要），AI 将综合身体健康与心理健康共同分析
     */
    @Operation(summary = "生成AI健康建议")
    @PostMapping("/health-advice")
    public R<?> healthAdvice(@RequestBody(required = false) Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");

        // 1. 查最新一条健康数据
        LambdaQueryWrapper<HealthData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthData::getUserId, userId)
                .orderByDesc(HealthData::getRecordDate)
                .last("LIMIT 1");
        HealthData latest = healthDataService.getOne(wrapper);

        if (latest == null) {
            return R.fail("您还没有录入健康数据，请先前往健康数据页面录入");
        }

        // 2. 拼接 prompt（身体健康）
        StringBuilder prompt = new StringBuilder("我的身体健康数据如下：\n");
        prompt.append("- 记录日期：").append(latest.getRecordDate()).append("\n");
        if (latest.getHeight() != null)  prompt.append("- 身高：").append(latest.getHeight()).append(" cm\n");
        if (latest.getWeight() != null)  prompt.append("- 体重：").append(latest.getWeight()).append(" kg\n");
        if (latest.getBmi() != null)     prompt.append("- BMI：").append(latest.getBmi()).append("\n");
        if (latest.getSystolicPressure() != null)
            prompt.append("- 血压：").append(latest.getSystolicPressure())
                    .append("/").append(latest.getDiastolicPressure()).append(" mmHg\n");
        if (latest.getHeartRate() != null) prompt.append("- 心率：").append(latest.getHeartRate()).append(" 次/分\n");
        if (latest.getSteps() != null)     prompt.append("- 步数：").append(latest.getSteps()).append(" 步\n");
        if (latest.getSleepHours() != null) prompt.append("- 睡眠：").append(latest.getSleepHours()).append(" 小时\n");

        // 2.1 拼接心理健康测评数据（前端传入，可选）
        Map<String, Object> mental = body != null ? (Map<String, Object>) body.get("mentalHealth") : null;
        if (mental != null && Boolean.TRUE.equals(mental.get("hasData"))) {
            prompt.append("\n我的心理健康测评数据（最近一次）如下：\n");
            if (mental.get("date") != null)
                prompt.append("- 测评日期：").append(mental.get("date")).append("\n");
            if (mental.get("wellness") != null)
                prompt.append("- 情绪健康分（0-100，越高越好）：").append(mental.get("wellness")).append("\n");
            if (mental.get("dep") != null)
                prompt.append("- 抑郁倾向(PHQ-9)得分：").append(mental.get("dep")).append("\n");
            if (mental.get("anx") != null)
                prompt.append("- 焦虑倾向(GAD-7)得分：").append(mental.get("anx")).append("\n");
            if (mental.get("sleep") != null)
                prompt.append("- 睡眠维度得分：").append(mental.get("sleep")).append("\n");
            if (mental.get("stress") != null)
                prompt.append("- 压力维度得分：").append(mental.get("stress")).append("\n");
            if (mental.get("isLongTermLow") != null)
                prompt.append("- 是否长期处于情绪低谷：")
                        .append(Boolean.TRUE.equals(mental.get("isLongTermLow")) ? "是" : "否").append("\n");
            prompt.append("\n请结合以上身体健康与心理健康，综合给出健康与情绪管理的建议。");
        } else {
            prompt.append("\n请根据以上身体健康数据给我健康建议。");
        }

        // 3. 调大模型
        try {
            String adviceText = aiService.getHealthAdvice(prompt.toString());

            // 👇 4. 立即保存到数据库
            AiAdvice advice = new AiAdvice();
            advice.setUserId(userId.intValue());
            advice.setBasedDataId(latest.getId());
            advice.setAdviceContent(adviceText);
            advice.setRecordDate(latest.getRecordDate());
            aiAdviceService.save(advice);

            // 5. 返回给前端（带上 id 和 createTime，方便前端展示）
            Map<String, Object> data = new HashMap<>();
            data.put("id", advice.getId());
            data.put("advice", adviceText);
            data.put("basedOn", latest);
            data.put("basedOnMental", mental);
            data.put("hasMental", mental != null && Boolean.TRUE.equals(mental.get("hasData")));
            data.put("createTime", advice.getCreateTime());
            return R.data(data);
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("AI 生成失败：" + e.getMessage());
        }
    }

    /**
     * 查询当前用户的所有历史建议（按时间倒序）
     */
    @Operation(summary = "查询历史AI建议")
    @PostMapping("/advice-history")
    public R<List<AiAdvice>> history() {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");

        LambdaQueryWrapper<AiAdvice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAdvice::getUserId, userId.intValue())
                .orderByDesc(AiAdvice::getCreateTime);
        List<AiAdvice> list = aiAdviceService.list(wrapper);

        return R.data(list);
    }

    /**
     * 删除某条历史建议（只能删自己的）
     */
    @Operation(summary = "删除历史AI建议")
    @PostMapping("/advice-del")
    public R<?> delete(@RequestParam Integer id) {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");

        AiAdvice advice = aiAdviceService.getById(id);
        if (advice == null) return R.fail("记录不存在");
        if (!advice.getUserId().equals(userId.intValue())) return R.fail("无权操作");

        aiAdviceService.removeById(id);
        return R.success("删除成功");
    }

    // ===================== 多轮对话（保存历史） =====================

    /**
     * 发送一条对话消息：带最近上下文调大模型，并把用户消息 + AI 回复落库
     */
    @Operation(summary = "AI对话：发送消息并获取回复，自动保存历史")
    @PostMapping("/chat")
    public R<?> chat(@RequestBody(required = false) Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");

        String message = body == null ? null : (String) body.get("message");
        if (message == null || message.trim().isEmpty()) return R.fail("消息内容不能为空");

        // 1. 取最近 20 条历史作为上下文（时间正序）
        LambdaQueryWrapper<AiChatMessage> w = new LambdaQueryWrapper<>();
        w.eq(AiChatMessage::getUserId, userId.intValue())
                .orderByDesc(AiChatMessage::getCreateTime)
                .last("LIMIT 20");
        List<AiChatMessage> recent = chatService.list(w);
        Collections.reverse(recent);

        List<Map<String, String>> conversation = new ArrayList<>();
        for (AiChatMessage m : recent) {
            Map<String, String> h = new HashMap<>(2);
            h.put("role", m.getRole());
            h.put("content", m.getContent());
            conversation.add(h);
        }
        // 追加当前这条用户消息
        Map<String, String> cur = new HashMap<>(2);
        cur.put("role", "user");
        cur.put("content", message);
        conversation.add(cur);

        // 2. 调大模型
        String reply;
        try {
            reply = aiService.chat(conversation);
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("AI 对话失败：" + e.getMessage());
        }

        // 3. 落库：用户消息 + AI 回复
        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setUserId(userId.intValue());
        userMsg.setRole("user");
        userMsg.setContent(message);
        chatService.save(userMsg);

        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setUserId(userId.intValue());
        aiMsg.setRole("assistant");
        aiMsg.setContent(reply);
        chatService.save(aiMsg);

        // 4. 返回回复
        Map<String, Object> data = new HashMap<>(1);
        data.put("reply", reply);
        return R.data(data);
    }

    /**
     * 查询当前用户的全部对话历史（时间正序）
     */
    @Operation(summary = "查询对话历史")
    @PostMapping("/chat-history")
    public R<?> chatHistory() {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");

        LambdaQueryWrapper<AiChatMessage> w = new LambdaQueryWrapper<>();
        w.eq(AiChatMessage::getUserId, userId.intValue())
                .orderByAsc(AiChatMessage::getCreateTime);
        return R.data(chatService.list(w));
    }

    /**
     * 清空当前用户的对话历史
     */
    @Operation(summary = "清空对话历史")
    @PostMapping("/chat-clear")
    public R<?> chatClear() {
        Long userId = UserContext.getUserId();
        if (userId == null) return R.fail("未登录");

        LambdaQueryWrapper<AiChatMessage> w = new LambdaQueryWrapper<>();
        w.eq(AiChatMessage::getUserId, userId.intValue());
        chatService.remove(w);
        return R.success("对话历史已清空");
    }
}