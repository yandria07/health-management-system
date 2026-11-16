package com.health.healthserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.health.healthserver.config.ZhipuConfig;
import jakarta.annotation.Resource;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AiService {

    @Resource
    private ZhipuConfig zhipuConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)     // 大模型响应可能较慢，给足时间
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /** 健康建议的系统提示词（综合身心健康，结构化输出） */
    private static final String ADVICE_SYSTEM_PROMPT =
            "你是一位专业的健康管理顾问，同时具备身心健康（生理+心理）综合分析能力。" +
                    "用户会提供他的身体健康数据（体重、BMI、血压、心率、步数、睡眠等），" +
                    "可能还会提供心理健康测评数据（情绪健康分、抑郁PHQ-9得分、焦虑GAD-7得分、睡眠/压力维度得分、是否长期情绪低谷等）。" +
                    "请你综合身体健康与心理健康，给出专业、温和、可操作的健康与情绪管理建议。" +
                    "要求：" +
                    "1. 用中文回答；" +
                    "2. 分点列出建议，每条简明扼要；" +
                    "3. 如果身体或心理数据异常，用友好的语气提醒，必要时温和建议寻求专业心理/医疗帮助；" +
                    "4. 总字数控制在 350 字以内；" +
                    "5. 不要重复用户给的数据，直接给建议。";

    /** 对话模式下的系统提示词（自然、口语化、可多轮） */
    private static final String CHAT_SYSTEM_PROMPT =
            "你是一位亲切、专业的个人健康管理助手，名字叫「小脉」。" +
                    "你可以就用户的身体健康、心理健康、饮食、运动、睡眠、压力等话题进行自然、连续的对话式交流，" +
                    "结合之前的对话内容给出温和、可操作、专业的建议。" +
                    "要求：" +
                    "1. 用中文回答，语气友好、口语化；" +
                    "2. 回复简明，必要时分点，避免长篇大论；" +
                    "3. 如果用户身体或心理数据/描述异常，用关心的语气提醒，必要时温和建议寻求专业医疗或心理帮助；" +
                    "4. 不要编造用户的健康数据，应基于用户所说的内容交流。";

    /**
     * 调用 GLM 生成健康建议（单轮：基于健康数据）
     */
    public String getHealthAdvice(String userPrompt) throws IOException {
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", ADVICE_SYSTEM_PROMPT);
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        return callZhipu(messages);
    }

    /**
     * 多轮对话：conversation 为已整理好的完整上下文（不含系统提示词），
     * 每条为 { role: "user"/"assistant", content: "..." }。
     */
    public String chat(List<Map<String, String>> conversation) throws IOException {
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", CHAT_SYSTEM_PROMPT);
        if (conversation != null) {
            for (Map<String, String> m : conversation) {
                if (m == null) continue;
                ObjectNode o = messages.addObject();
                o.put("role", m.get("role"));
                o.put("content", m.get("content"));
            }
        }
        return callZhipu(messages);
    }

    /** 体检报告解读的系统提示词：通俗语言，不堆砌医学术语 */
    private static final String EXAM_INTERPRET_SYSTEM_PROMPT =
            "你是一位耐心、亲切的全科健康顾问，正在帮普通用户解读一份体检报告。" +
                    "用户会把体检的异常指标（项目名称、数值、参考范围、偏高还是偏低）告诉你，" +
                    "请你用普通人能听懂的大白话解读，避免堆砌医学专业术语。" +
                    "请严格按以下三个部分输出，每部分用【】标题开头，内容分点列出，整体控制在 500 字以内：" +
                    "【异常指标说明】逐条说明每项异常可能意味着什么、日常常见原因，语气客观不吓人；" +
                    "【生活建议】给出具体可操作的饮食、运动、作息、情绪管理建议；" +
                    "【复查提醒】说明哪些指标需要多久后复查、什么情况需要就医。" +
                    "如果异常项很少或没有，也要给出保持健康生活方式的鼓励。" +
                    "重要：你不是医生，请在结尾用一句话提醒「以上建议仅供参考，具体请咨询医生」。";

    /**
     * 解读体检报告：把异常项交给大模型，输出通俗的异常说明、生活建议、复查提醒
     */
    public String interpretExam(String examAbnormalInfo) throws IOException {
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", EXAM_INTERPRET_SYSTEM_PROMPT);
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", "以下是我这份体检报告的指标情况，请帮我解读：\n" + examAbnormalInfo);
        return callZhipu(messages);
    }

    /**
     * 通用调用：messages 已包含 system + 全部对话内容
     */
    private String callZhipu(ArrayNode messages) throws IOException {
        // 1. 构造请求体 JSON
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", zhipuConfig.getModel());
        root.put("stream", false);                  // 非流式，一次性返回
        root.put("temperature", 0.7);               // 0-1，越大越有创造性
        root.set("messages", messages);

        // 2. 构造 HTTP 请求
        RequestBody body = RequestBody.create(
                root.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(zhipuConfig.getApiUrl())
                .addHeader("Authorization", "Bearer " + zhipuConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // 3. 发送请求并解析响应
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "无响应体";
                throw new RuntimeException("调用大模型失败: " + response.code() + " - " + errBody);
            }

            String respBody = response.body().string();
            JsonNode respJson = objectMapper.readTree(respBody);

            // 智谱返回格式：{ choices: [ { message: { content: "..." } } ] }
            return respJson.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();
        }
    }
}
