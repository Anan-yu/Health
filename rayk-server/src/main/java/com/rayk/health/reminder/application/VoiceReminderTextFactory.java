package com.rayk.health.reminder.application;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class VoiceReminderTextFactory {
    private static final List<String> MEAL_TEMPLATES =
            List.of(
                    "%s，到吃饭时间啦。再忙也要先照顾好自己，认真吃饭的人，今天也会更有能量。",
                    "%s，饭点到啦。胃已经准时打卡，就等你来照顾它了，记得慢慢吃、吃舒服。",
                    "%s，该吃饭啦。今天的健康小任务很简单：好好吃一顿，给身体补充能量。",
                    "%s，先暂停一下手里的事吧。按时吃饭不是小事，是在认真守护自己的健康。",
                    "%s，饭菜要趁热，关心也要及时。现在去吃饭，别让身体等太久。",
                    "%s，到饭点了。愿你这一餐吃得香、吃得暖，也把今天的疲惫轻轻放下。",
                    "%s，健康提醒来敲门啦。记得按时吃饭，荤素搭配，细嚼慢咽更舒服。",
                    "%s，再忙也别把吃饭排到最后。先给身体充好电，后面的事情才更有力气完成。"
            );

    private static final List<String> SLEEP_TEMPLATES =
            List.of(
                    "%s，夜深啦。把今天的辛苦先放下，早点休息，明天醒来又是精神满满的一天。",
                    "%s，该准备睡觉啦。手机也需要充电，你当然更需要，今晚早点和枕头见面吧。",
                    "%s，今天已经很努力了。现在把时间交给睡眠，让身体安安静静地恢复能量。",
                    "%s，睡觉时间到啦。放下手机，做几个深呼吸，愿你今晚睡得安稳又香甜。",
                    "%s，别再和困意讲道理啦。早点休息，是送给明天自己的一份健康礼物。",
                    "%s，夜晚是身体的修复时间。关掉忙碌模式，好好睡一觉，明天会更轻松。",
                    "%s，该休息了。今天没做完的事可以明天继续，健康和睡眠要放在心上。",
                    "%s，温馨提醒：今晚的最佳安排，是洗漱、放松，然后安心睡个好觉。"
            );

    public String create(String type, String patientName) {
        String name = patientName == null || patientName.isBlank() ? "朋友" : patientName.trim();
        List<String> templates = "SLEEP".equals(type) ? SLEEP_TEMPLATES : MEAL_TEMPLATES;
        String template = templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
        return template.formatted(name);
    }
}
