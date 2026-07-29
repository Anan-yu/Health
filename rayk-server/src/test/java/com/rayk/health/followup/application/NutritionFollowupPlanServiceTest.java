package com.rayk.health.followup.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.followup.dto.FollowupActionFeedback;
import com.rayk.health.integration.ai.AiDtos;
import com.rayk.health.patient.vo.HealthProfileVo;
import java.util.List;
import org.junit.jupiter.api.Test;

class NutritionFollowupPlanServiceTest {
    private final NutritionFollowupPlanService service =
            new NutritionFollowupPlanService(new ObjectMapper());

    @Test
    void buildsChronicDiseaseAwareFoodFirstPlan() {
        String plan = service.buildInitialPlan(null, chronicProfile(), 1);

        assertThat(plan)
                .contains("营养目标")
                .contains("微量营养建议")
                .contains("一周营养食谱")
                .contains("周一｜早餐")
                .contains("糖代谢管理重点")
                .contains("高血压饮食重点")
                .contains("维生素B12")
                .contains("无糖强化豆饮")
                .contains("去皮鸡肉")
                .doesNotContain("补充剂安全")
                .doesNotContain("本计划不提供补充剂剂量");
    }

    @Test
    void refreshesMenuAndCarriesOnlyUnfinishedActionsWhenAdjusted() {
        List<FollowupActionFeedback> actions =
                List.of(
                        new FollowupActionFeedback(
                                "运动行动", "快走30分钟", "COMPLETED", null),
                        new FollowupActionFeedback(
                                "作息行动", "23点前入睡", "PARTIAL", "完成了三天"),
                        new FollowupActionFeedback(
                                "一周营养食谱", "周一食谱", "NOT_COMPLETED", "临时外出"));

        String plan = service.buildNextPlan(chronicProfile(), actions, true, 2);

        assertThat(plan)
                .contains("一周营养食谱")
                .contains("周一｜")
                .contains("优先完成：23点前入睡")
                .doesNotContain("优先完成：快走30分钟")
                .doesNotContain("优先完成：周一食谱");
    }

    @Test
    void usesFeedbackAwareAiSuggestionsInsteadOfRepeatingPreviousActions() {
        List<FollowupActionFeedback> actions =
                List.of(
                        new FollowupActionFeedback(
                                "运动行动", "每周快走5次，每次30分钟", "PARTIAL", "膝盖疼"));
        List<AiDtos.FollowupActionSuggestion> suggestions =
                List.of(
                        new AiDtos.FollowupActionSuggestion(
                                "运动行动", "本周改为坐姿抬腿，每周3次，每次10分钟；不适时停止。"),
                        new AiDtos.FollowupActionSuggestion(
                                "监测行动", "每天记录膝盖不适出现的时间和持续时长。"));

        String plan = service.buildNextPlan(chronicProfile(), actions, suggestions, true, 2);

        assertThat(plan)
                .contains("本周改为坐姿抬腿")
                .contains("每天记录膝盖不适")
                .doesNotContain("每周快走5次");
    }

    private HealthProfileVo chronicProfile() {
        return new HealthProfileVo(
                "1",
                "10001",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "缺铁性贫血",
                null,
                "牛奶、海鲜",
                "二甲双胍、华法林",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "YES",
                "YES",
                "NO",
                "NO",
                60,
                null);
    }
}
