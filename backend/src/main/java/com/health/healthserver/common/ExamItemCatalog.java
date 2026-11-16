package com.health.healthserver.common;

import java.math.BigDecimal;
import java.util.*;

/**
 * 体检指标目录（数据字典）
 * 结合常见医院体检报告，筛选 6 大类 22 项核心指标。
 * 每项含参考范围，后端据此自动判定 正常/偏高/偏低。
 * 参考范围为通用成人区间，实际以各医院/性别为准，仅作健康自查提示。
 */
public final class ExamItemCatalog {

    /** 单项指标元数据 */
    public static final class ExamItem {
        private final String key;
        private final String name;
        private final String category;
        private final String categoryLabel;
        private final String unit;
        private final BigDecimal refLow;
        private final BigDecimal refHigh;
        private final String tip;

        public ExamItem(String key, String name, String category, String categoryLabel,
                        String unit, String low, String high, String tip) {
            this.key = key;
            this.name = name;
            this.category = category;
            this.categoryLabel = categoryLabel;
            this.unit = unit;
            this.refLow = low == null ? null : new BigDecimal(low);
            this.refHigh = high == null ? null : new BigDecimal(high);
            this.tip = tip;
        }

        public String getKey() { return key; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getCategoryLabel() { return categoryLabel; }
        public String getUnit() { return unit; }
        public BigDecimal getRefLow() { return refLow; }
        public BigDecimal getRefHigh() { return refHigh; }
        public String getTip() { return tip; }

        /** 参考范围文本，如 "18.5~23.9" / "≥1.0" / "≤5.2" / "—" */
        public String getRefText() {
            if (refLow == null && refHigh == null) return "—";
            if (refLow == null) return "≤ " + plain(refHigh);
            if (refHigh == null) return "≥ " + plain(refLow);
            return plain(refLow) + " ~ " + plain(refHigh);
        }

        private static String plain(BigDecimal d) {
            return d.stripTrailingZeros().toPlainString();
        }
    }

    /** 分类（有序） */
    public static final List<Map<String, String>> CATEGORIES = new ArrayList<>();
    static {
        CATEGORIES.add(cat("general", "一般检查"));
        CATEGORIES.add(cat("blood", "血常规"));
        CATEGORIES.add(cat("liver", "肝功能"));
        CATEGORIES.add(cat("kidney", "肾功能"));
        CATEGORIES.add(cat("lipid", "血脂"));
        CATEGORIES.add(cat("glucose", "血糖"));
    }
    private static Map<String, String> cat(String key, String label) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        return m;
    }

    /** 22 项核心指标 */
    public static final List<ExamItem> ITEMS = new ArrayList<>();
    static {
        // —— 一般检查 ——
        ITEMS.add(new ExamItem("height", "身高", "general", "一般检查", "cm", null, null, "体检时的身高记录。"));
        ITEMS.add(new ExamItem("weight", "体重", "general", "一般检查", "kg", null, null, "体检时的体重记录。"));
        ITEMS.add(new ExamItem("bmi", "体重指数 BMI", "general", "一般检查", "", "18.5", "23.9", "衡量体重是否适中，过高或过低都需关注。"));
        ITEMS.add(new ExamItem("sbp", "收缩压（高压）", "general", "一般检查", "mmHg", "90", "139", "血压的高值，长期偏高需警惕高血压。"));
        ITEMS.add(new ExamItem("dbp", "舒张压（低压）", "general", "一般检查", "mmHg", "60", "89", "血压的低值。"));
        ITEMS.add(new ExamItem("pulse", "脉搏", "general", "一般检查", "次/分", "60", "100", "安静状态下的心跳次数。"));

        // —— 血常规 ——
        ITEMS.add(new ExamItem("wbc", "白细胞计数 WBC", "blood", "血常规", "×10⁹/L", "3.5", "9.5", "偏高常提示感染或炎症，偏低可能与免疫力相关。"));
        ITEMS.add(new ExamItem("rbc", "红细胞计数 RBC", "blood", "血常规", "×10¹²/L", "4.0", "5.8", "偏低常见于贫血。"));
        ITEMS.add(new ExamItem("hgb", "血红蛋白 HGB", "blood", "血常规", "g/L", "120", "160", "判断贫血的重要指标，偏低需关注。"));
        ITEMS.add(new ExamItem("plt", "血小板 PLT", "blood", "血常规", "×10⁹/L", "125", "350", "与凝血功能相关，过高过低都需留意。"));

        // —— 肝功能 ——
        ITEMS.add(new ExamItem("alt", "谷丙转氨酶 ALT", "liver", "肝功能", "U/L", "0", "40", "偏高常提示肝细胞损伤、脂肪肝或饮酒影响。"));
        ITEMS.add(new ExamItem("ast", "谷草转氨酶 AST", "liver", "肝功能", "U/L", "0", "40", "与 ALT 一起反映肝脏健康状况。"));
        ITEMS.add(new ExamItem("tbil", "总胆红素 TBIL", "liver", "肝功能", "μmol/L", "3.4", "20.5", "偏高可能与肝胆代谢、溶血或熬夜有关。"));

        // —— 肾功能 ——
        ITEMS.add(new ExamItem("cr", "血肌酐 Cr", "kidney", "肾功能", "μmol/L", "44", "106", "反映肾脏滤过功能，偏高需重视。"));
        ITEMS.add(new ExamItem("bun", "尿素氮 BUN", "kidney", "肾功能", "mmol/L", "3.1", "8.0", "与肌酐一起评估肾功能。"));
        ITEMS.add(new ExamItem("ua", "尿酸 UA", "kidney", "肾功能", "μmol/L", "150", "430", "偏高与痛风、饮食相关，需控制嘌呤摄入。"));

        // —— 血脂 ——
        ITEMS.add(new ExamItem("tc", "总胆固醇 TC", "lipid", "血脂", "mmol/L", "0", "5.2", "偏高提示血脂异常，注意饮食与运动。"));
        ITEMS.add(new ExamItem("tg", "甘油三酯 TG", "lipid", "血脂", "mmol/L", "0", "1.7", "偏高与油腻饮食、饮酒、缺乏运动相关。"));
        ITEMS.add(new ExamItem("ldl", "低密度脂蛋白 LDL-C", "lipid", "血脂", "mmol/L", "0", "3.4", "俗称“坏胆固醇”，偏高需控制。"));
        ITEMS.add(new ExamItem("hdl", "高密度脂蛋白 HDL-C", "lipid", "血脂", "mmol/L", "1.0", null, "俗称“好胆固醇”，偏低反而不利，宜适当运动提升。"));

        // —— 血糖 ——
        ITEMS.add(new ExamItem("glu", "空腹血糖 GLU", "glucose", "血糖", "mmol/L", "3.9", "6.1", "偏高提示血糖异常，需复查糖代谢。"));
        ITEMS.add(new ExamItem("hba1c", "糖化血红蛋白 HbA1c", "glucose", "血糖", "%", "4.0", "6.0", "反映近三个月平均血糖，比单次血糖更稳定。"));
    }

    private ExamItemCatalog() {}

    /** 供前端渲染的目录：分类 + 指标（含参考范围文本） */
    public static Map<String, Object> getCatalog() {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> itemList = new ArrayList<>();
        for (ExamItem it : ITEMS) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", it.getKey());
            m.put("name", it.getName());
            m.put("category", it.getCategory());
            m.put("categoryLabel", it.getCategoryLabel());
            m.put("unit", it.getUnit());
            m.put("refLow", it.getRefLow());
            m.put("refHigh", it.getRefHigh());
            m.put("refText", it.getRefText());
            m.put("tip", it.getTip());
            itemList.add(m);
        }
        data.put("categories", CATEGORIES);
        data.put("items", itemList);
        return data;
    }

    /** 按 key 查指标 */
    public static ExamItem getItem(String key) {
        for (ExamItem it : ITEMS) {
            if (it.getKey().equals(key)) return it;
        }
        return null;
    }

    /**
     * 判定单项状态：normal 正常 / high 偏高 / low 偏低 / none 未录入或无参考范围
     */
    public static String judge(ExamItem item, BigDecimal value) {
        if (item == null || value == null) return "none";
        if (item.getRefLow() == null && item.getRefHigh() == null) return "none";
        if (item.getRefLow() != null && value.compareTo(item.getRefLow()) < 0) return "low";
        if (item.getRefHigh() != null && value.compareTo(item.getRefHigh()) > 0) return "high";
        return "normal";
    }

    /**
     * 构造带状态的详情列表（供详情接口返回）
     */
    public static List<Map<String, Object>> buildDetail(Map<String, Object> values) {
        List<Map<String, Object>> detail = new ArrayList<>();
        for (ExamItem it : ITEMS) {
            Object raw = values == null ? null : values.get(it.getKey());
            BigDecimal value = null;
            if (raw instanceof Number) {
                value = new BigDecimal(raw.toString());
            } else if (raw instanceof String && !((String) raw).trim().isEmpty()) {
                try { value = new BigDecimal(((String) raw).trim()); } catch (NumberFormatException ignored) {}
            }
            String status = judge(it, value);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", it.getKey());
            m.put("name", it.getName());
            m.put("category", it.getCategory());
            m.put("categoryLabel", it.getCategoryLabel());
            m.put("unit", it.getUnit());
            m.put("refText", it.getRefText());
            m.put("tip", it.getTip());
            m.put("value", value);
            m.put("status", status);
            detail.add(m);
        }
        return detail;
    }

    /**
     * 生成异常项描述文本（供 AI 解读 prompt 使用）
     */
    public static String buildAbnormalText(Map<String, Object> values) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (ExamItem it : ITEMS) {
            Object raw = values == null ? null : values.get(it.getKey());
            BigDecimal value = null;
            if (raw instanceof Number) value = new BigDecimal(raw.toString());
            else if (raw instanceof String && !((String) raw).trim().isEmpty()) {
                try { value = new BigDecimal(((String) raw).trim()); } catch (NumberFormatException ignored) {}
            }
            String status = judge(it, value);
            if ("high".equals(status) || "low".equals(status)) {
                count++;
                sb.append("- ").append(it.getName())
                  .append("：").append(value.stripTrailingZeros().toPlainString())
                  .append(" ").append(it.getUnit())
                  .append("，参考范围 ").append(it.getRefText())
                  .append("，").append("high".equals(status) ? "偏高" : "偏低").append("\n");
            }
        }
        if (count == 0) return "本次体检所有录入项均在参考范围内，未见明显异常。";
        return "共 " + count + " 项异常：\n" + sb.toString();
    }
}
