package com.rayk.health.followup.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.assessment.entity.HealthAssessmentEntity;
import com.rayk.health.followup.dto.FollowupActionFeedback;
import com.rayk.health.patient.vo.HealthProfileVo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Builds food-first nutrition follow-up plans from the health profile and assessment result.
 *
 * <p>The service deliberately does not prescribe supplement doses. A supplement is only suggested
 * after a documented deficiency or professional confirmation, and common chronic-disease,
 * medication and allergy constraints are applied before a weekly menu is rendered.
 */
@Service
public class NutritionFollowupPlanService {
    private static final Set<String> GENERATED_NUTRITION_SECTIONS =
            Set.of("营养目标", "微量营养建议", "一周营养食谱");

    private final ObjectMapper objectMapper;

    public NutritionFollowupPlanService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildInitialPlan(
            HealthAssessmentEntity assessment, HealthProfileVo profile, int cycleNo) {
        NutritionContext context = analyze(profile, assessment);
        LinkedHashMap<String, List<String>> sections = new LinkedHashMap<>();
        sections.put("营养目标", nutritionGoals(context));
        sections.put("微量营养建议", micronutrientAdvice(context));
        sections.put("一周营养食谱", weeklyMenu(context, cycleNo));
        sections.put("饮食行动", dietActions(context));
        sections.put(
                "运动行动",
                List.of(
                        "在身体允许的情况下，每周5天进行快走或同等强度运动，每次约30分钟。",
                        "每周安排2次轻量力量训练，每次15至20分钟；如有不适立即停止。"));
        sections.put(
                "作息行动",
                List.of(
                        "固定上床和起床时间，每晚争取睡足7至9小时。",
                        "睡前1小时减少手机使用，晚餐尽量在睡前3小时完成。"));
        sections.put("监测行动", monitoringActions(context));
        return render(sections);
    }

    public String buildNextPlan(
            HealthProfileVo profile,
            List<FollowupActionFeedback> actions,
            boolean adjusted,
            int cycleNo) {
        return buildNextPlan(profile, actions, List.of(), adjusted, cycleNo);
    }

    public String buildNextPlan(
            HealthProfileVo profile,
            List<FollowupActionFeedback> actions,
            List<com.rayk.health.integration.ai.AiDtos.FollowupActionSuggestion> suggestions,
            boolean adjusted,
            int cycleNo) {
        NutritionContext context = analyze(profile, null);
        LinkedHashMap<String, List<String>> sections = new LinkedHashMap<>();
        sections.put("营养目标", nutritionGoals(context));
        sections.put("微量营养建议", micronutrientAdvice(context));
        sections.put("一周营养食谱", weeklyMenu(context, cycleNo));

        if (suggestions != null && !suggestions.isEmpty()) {
            for (com.rayk.health.integration.ai.AiDtos.FollowupActionSuggestion suggestion :
                    suggestions) {
                String section = normalized(suggestion.section());
                String text = normalized(suggestion.action());
                if (section.isEmpty()
                        || text.isEmpty()
                        || GENERATED_NUTRITION_SECTIONS.contains(section)) {
                    continue;
                }
                sections.computeIfAbsent(section, ignored -> new ArrayList<>()).add(text);
            }
        } else {
            for (FollowupActionFeedback action : actions) {
                String section = normalized(action.section());
                if (section.isEmpty() || GENERATED_NUTRITION_SECTIONS.contains(section)) {
                    continue;
                }
                if (adjusted && "COMPLETED".equals(action.status())) {
                    continue;
                }
                String text = normalized(action.action());
                if (text.isEmpty()) {
                    continue;
                }
                if (adjusted) {
                    text = "优先完成：" + text;
                }
                sections.computeIfAbsent(section, ignored -> new ArrayList<>()).add(text);
            }
        }
        sections.putIfAbsent("饮食行动", dietActions(context));
        sections.putIfAbsent("监测行动", monitoringActions(context));
        return render(sections);
    }

    private NutritionContext analyze(
            HealthProfileVo profile, HealthAssessmentEntity assessment) {
        String medicalHistory = normalized(profile == null ? null : profile.medicalHistory());
        String medications = normalized(profile == null ? null : profile.currentMedications());
        String allergies = normalized(profile == null ? null : profile.allergyHistory());
        String combined = (medicalHistory + " " + medications).toLowerCase(Locale.ROOT);
        Set<String> focusCodes = assessmentFocusCodes(assessment);

        boolean diabetes =
                positive(profile == null ? null : profile.diabetesStatus())
                        || containsAny(combined, "糖尿病", "高血糖", "二甲双胍", "胰岛素");
        boolean hypertension =
                positive(profile == null ? null : profile.hypertensionStatus())
                        || containsAny(combined, "高血压", "降压药");
        boolean dyslipidemia =
                positive(profile == null ? null : profile.dyslipidemiaStatus())
                        || containsAny(combined, "高脂血症", "血脂异常", "高胆固醇");
        boolean fattyLiver =
                positive(profile == null ? null : profile.fattyLiverStatus())
                        || containsAny(combined, "脂肪肝");
        boolean kidneyDisease =
                containsAny(combined, "慢性肾", "肾功能不全", "肾衰", "透析", "肾病");
        boolean gout = containsAny(combined, "痛风", "高尿酸", "尿酸高");
        boolean anemia =
                containsAny(combined, "贫血", "缺铁")
                        || focusCodes.contains("HEMATOLOGY_ANEMIA");
        boolean boneRisk =
                containsAny(combined, "骨质疏松", "骨量减少", "维生素d缺乏", "低钙");
        boolean thyroidDisease =
                containsAny(combined, "甲亢", "甲减", "甲状腺炎", "甲状腺结节");
        boolean warfarin = containsAny(medications.toLowerCase(Locale.ROOT), "华法林");
        boolean metformin = containsAny(medications.toLowerCase(Locale.ROOT), "二甲双胍");
        boolean nutritionRisk =
                focusCodes.contains("NUTRITION_MICRONUTRIENT")
                        || focusCodes.contains("HEMATOLOGY_ANEMIA");

        return new NutritionContext(
                diabetes,
                hypertension,
                dyslipidemia,
                fattyLiver,
                kidneyDisease,
                gout,
                anemia,
                boneRisk,
                thyroidDisease,
                warfarin,
                metformin,
                nutritionRisk,
                containsAny(allergies, "牛奶", "乳制品", "乳糖"),
                containsAny(allergies, "鸡蛋", "蛋类"),
                containsAny(allergies, "海鲜", "鱼", "虾", "蟹"),
                containsAny(allergies, "大豆", "豆制品", "黄豆"),
                containsAny(allergies, "花生", "坚果"),
                allergies);
    }

    private List<String> nutritionGoals(NutritionContext context) {
        List<String> goals = new ArrayList<>();
        if (context.hypertension()) {
            goals.add("高血压饮食重点：少盐、少腌制食品，优先使用蒸、煮、炖等清淡烹调方式。");
        }
        if (context.diabetes()) {
            goals.add("糖代谢管理重点：三餐规律，主食定量并优先选择全谷物，避免含糖饮料和集中摄入甜食。");
        }
        if (context.dyslipidemia() || context.fattyLiver()) {
            goals.add("血脂与脂肪肝管理重点：减少油炸食品、肥肉和动物内脏，增加蔬菜、全谷物和适量优质蛋白。");
        }
        if (context.kidneyDisease()) {
            goals.add("肾脏病史已纳入约束：蛋白质、钾、磷和饮水量需结合肾功能及医嘱个体化调整。");
        }
        if (context.gout()) {
            goals.add("尿酸管理重点：不饮酒，少吃动物内脏、浓肉汤和高嘌呤海鲜，饮水量按医生建议执行。");
        }
        if (!context.recognizedChronicCondition()) {
            goals.add("保持食物多样、三餐规律，每餐包含蔬菜、优质蛋白和适量主食。");
        }
        return goals.stream().distinct().limit(4).toList();
    }

    private List<String> micronutrientAdvice(NutritionContext context) {
        List<String> advice = new ArrayList<>();
        if (context.anemia() || context.nutritionRisk()) {
            advice.add(
                    "铁、叶酸和维生素B12：优先从瘦肉、蛋、深色蔬菜和强化食品获取；补充前结合血常规、铁蛋白、维生素B12和叶酸检查由医生确认。");
        }
        if (context.metformin()) {
            advice.add("维生素B12：长期使用二甲双胍者可与医生讨论定期检测，仅在缺乏或医嘱明确时使用补充剂。");
        }
        if (context.boneRisk() || (!context.dairyAllergy() && !context.kidneyDisease())) {
            advice.add("钙和维生素D：优先安排奶类或强化替代饮品、豆制品和适度日晒；肾病、结石或高钙血症者补充前须先咨询医生。");
        }
        if (context.hypertension() && !context.kidneyDisease()) {
            advice.add("钾和镁：优先来自新鲜蔬菜、水果、豆类和全谷物，不自行使用钾盐或钾、镁补充剂。");
        }
        if (context.dyslipidemia() || context.fattyLiver()) {
            advice.add("Omega-3脂肪酸：优先每周安排鱼类等食物来源；使用鱼油前需结合抗凝药物、出血风险和医生意见。");
        }
        if (context.thyroidDisease()) {
            advice.add("碘：已有甲状腺疾病时不自行使用含碘补充剂，碘盐和海产品摄入按医生建议调整。");
        }
        if (advice.isEmpty()) {
            advice.add("维生素和矿物质以食物来源为主；没有缺乏证据时，不建议自行长期使用高剂量单一营养素。");
        }
        return advice.stream().distinct().limit(5).toList();
    }

    private List<String> supplementSafety(NutritionContext context) {
        List<String> safety = new ArrayList<>();
        safety.add("本计划不提供补充剂剂量；仅在检验提示缺乏或医生确认后选择具体产品和用量。");
        if (context.kidneyDisease()) {
            safety.add("有肾脏病史时，不自行补充钾、镁、磷、维生素A或维生素D，也不要使用成分不明的复合补充剂。");
        }
        if (context.warfarin()) {
            safety.add("正在使用华法林时，维生素K来源需保持相对稳定，不自行使用维生素K或复合维生素补充剂。");
        }
        if (!context.allergyHistory().isEmpty()
                && !containsAny(context.allergyHistory(), "无", "否", "不详")) {
            safety.add("已根据档案避开可识别的食物过敏原；购买预包装食品时仍需核对配料表。");
        }
        safety.add("如出现不适、血糖或血压明显异常，停止新增补充剂并及时咨询医生。");
        return safety;
    }

    private List<String> weeklyMenu(NutritionContext context, int cycleNo) {
        String milk =
                context.dairyAllergy()
                        ? (context.soyAllergy() ? "无糖燕麦饮" : "无糖强化豆饮")
                        : "低脂牛奶";
        String egg = context.eggAllergy() ? "瘦肉片" : "水煮蛋";
        String fish = context.seafoodAllergy() ? "去皮鸡肉" : "清蒸鱼";
        String soy = context.soyAllergy() ? "瘦肉" : "豆腐";
        String nuts = context.nutAllergy() ? "低糖水果" : "少量原味坚果";
        String rice =
                context.kidneyDisease()
                        ? "米饭"
                        : context.diabetes() ? "杂粮饭（控制主食量）" : "杂粮饭";
        String bread = context.diabetes() ? "无糖全麦面包" : "全麦面包";
        String seasoning = context.hypertension() ? "少盐" : "清淡";

        List<String> menus =
                List.of(
                        "周一｜早餐："
                                + bread
                                + "、"
                                + egg
                                + "、黄瓜、"
                                + milk
                                + "；午餐："
                                + rice
                                + "、"
                                + fish
                                + "、西兰花；晚餐：小米饭、"
                                + soy
                                + "、菌菇青菜。",
                        "周二｜早餐：燕麦饭、"
                                + egg
                                + "、番茄；午餐："
                                + rice
                                + "、去皮鸡肉、两种蔬菜；晚餐：玉米、"
                                + fish
                                + "、冬瓜青菜。",
                        "周三｜早餐："
                                + bread
                                + "、"
                                + milk
                                + "、"
                                + nuts
                                + "；午餐：荞麦面、"
                                + soy
                                + "、绿叶菜；晚餐："
                                + rice
                                + "、瘦肉、彩椒木耳。",
                        "周四｜早餐：玉米、"
                                + egg
                                + "、"
                                + milk
                                + "；午餐："
                                + rice
                                + "、"
                                + fish
                                + "、芹菜胡萝卜；晚餐：山药、"
                                + soy
                                + "、清炒时蔬。",
                        "周五｜早餐：燕麦饭、"
                                + egg
                                + "、生菜；午餐："
                                + rice
                                + "、去皮鸡肉、番茄菜花；晚餐："
                                + bread
                                + "、"
                                + fish
                                + "、蔬菜汤。",
                        "周六｜早餐："
                                + bread
                                + "、"
                                + milk
                                + "、低糖水果；午餐："
                                + rice
                                + "、瘦肉、两种蔬菜；晚餐：南瓜、"
                                + soy
                                + "、凉拌蔬菜。",
                        "周日｜早餐：玉米、"
                                + egg
                                + "、番茄；午餐："
                                + rice
                                + "、"
                                + fish
                                + "、绿叶菜；晚餐：小米饭、去皮鸡肉、菌菇蔬菜。");
        int offset = Math.floorMod(cycleNo - 1, menus.size());
        List<String> rotated = new ArrayList<>();
        for (int index = 0; index < menus.size(); index++) {
            String menu = menus.get((index + offset) % menus.size());
            String day = "周" + "一二三四五六日".charAt(index);
            rotated.add(day + menu.substring(menu.indexOf('｜')) + "（" + seasoning + "烹调）");
        }
        return rotated;
    }

    private List<String> dietActions(NutritionContext context) {
        List<String> actions = new ArrayList<>();
        actions.add("每天三餐尽量定时，按食谱执行时可在同类食物间等量替换。");
        actions.add("每天至少记录一次实际饮食；无法完全执行时，优先做到少盐、少糖、少油和食物多样。");
        if (context.diabetes()) {
            actions.add("记录餐前或餐后血糖时，同时记录对应餐次主食和甜食摄入，便于观察变化。");
        }
        if (context.hypertension()) {
            actions.add("不额外添加咸菜、腌制品和高盐调味汁，外出就餐主动要求少盐。");
        }
        if (context.gout()) {
            actions.add("本周不饮酒，不吃动物内脏和浓肉汤。");
        }
        return actions.stream().distinct().limit(4).toList();
    }

    private List<String> monitoringActions(NutritionContext context) {
        List<String> actions = new ArrayList<>();
        actions.add("每天记录饮食执行情况、运动时长、睡眠时长和身体感受。");
        if (context.diabetes()) {
            actions.add("按既有医嘱监测血糖；如反复出现明显高值或低血糖症状，及时联系医生。");
        }
        if (context.hypertension()) {
            actions.add("按既有医嘱监测血压，记录测量时间和数值，复诊时提供趋势。");
        }
        actions.add("正在使用的药物继续按医嘱执行，不因食谱或补充剂建议自行增减、停药。");
        return actions.stream().distinct().limit(4).toList();
    }

    private Set<String> assessmentFocusCodes(HealthAssessmentEntity assessment) {
        if (assessment == null || assessment.getResultSnapshot() == null) {
            return Set.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        try {
            JsonNode results = objectMapper.readTree(assessment.getResultSnapshot()).path("results");
            for (JsonNode item : results) {
                if (Set.of("ATTENTION", "HIGH").contains(item.path("riskLevel").asText())) {
                    codes.add(item.path("modelCode").asText());
                }
            }
        } catch (Exception ignored) {
            return Set.of();
        }
        return codes;
    }

    private String render(Map<String, List<String>> sections) {
        StringBuilder content = new StringBuilder();
        sections.forEach(
                (title, actions) -> {
                    List<String> unique =
                            actions.stream()
                                    .map(this::normalized)
                                    .filter(value -> !value.isEmpty())
                                    .distinct()
                                    .toList();
                    if (unique.isEmpty()) {
                        return;
                    }
                    if (!content.isEmpty()) {
                        content.append('\n');
                    }
                    content.append(title).append('\n');
                    unique.forEach(action -> content.append("• ").append(action).append('\n'));
                });
        return content.toString().trim();
    }

    private boolean positive(String value) {
        String normalized = normalized(value).toUpperCase(Locale.ROOT);
        return Set.of("YES", "TRUE", "POSITIVE", "CONFIRMED", "DIAGNOSED", "有", "是", "确诊")
                .contains(normalized);
    }

    private boolean containsAny(String source, String... terms) {
        if (source == null || source.isBlank()) {
            return false;
        }
        String normalized = source.toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (normalized.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    private record NutritionContext(
            boolean diabetes,
            boolean hypertension,
            boolean dyslipidemia,
            boolean fattyLiver,
            boolean kidneyDisease,
            boolean gout,
            boolean anemia,
            boolean boneRisk,
            boolean thyroidDisease,
            boolean warfarin,
            boolean metformin,
            boolean nutritionRisk,
            boolean dairyAllergy,
            boolean eggAllergy,
            boolean seafoodAllergy,
            boolean soyAllergy,
            boolean nutAllergy,
            String allergyHistory) {
        private boolean recognizedChronicCondition() {
            return diabetes
                    || hypertension
                    || dyslipidemia
                    || fattyLiver
                    || kidneyDisease
                    || gout;
        }
    }
}
