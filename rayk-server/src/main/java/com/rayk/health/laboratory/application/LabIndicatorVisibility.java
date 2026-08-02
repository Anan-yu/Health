package com.rayk.health.laboratory.application;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Prevent report identity/profile metadata from being exposed as laboratory indicators. */
public final class LabIndicatorVisibility {
    private static final Set<String> EXACT_METADATA =
            Set.of(
                    "姓名", "性别", "年龄", "出生日期", "体检日期", "检查日期", "报告日期",
                    "打印日期", "送检日期", "采样日期", "咨询电话", "联系电话", "手机号码",
                    "手机号", "身份证号", "报告编号", "报告号", "体检编号", "住院号", "门诊号");

    private static final List<String> CONTAINS_METADATA =
            List.of(
                    "姓名", "性别", "年龄", "出生日期", "体检日期", "检查日期", "报告日期",
                    "打印日期", "送检日期", "采样日期", "报告时间", "咨询电话", "联系电话",
                    "手机号码", "手机号", "身份证", "报告编号", "报告号", "体检编号", "住院号",
                    "门诊号", "床位号", "床号", "条码号", "检查号", "检验号", "样本号",
                    "申请单号", "仪器型号", "设备编号");

    private static final List<String> PROFILE_PREFIXES =
            List.of(
                    "身高", "体重", "腰围", "臀围", "体脂率", "bmi", "体质指数", "血压",
                    "脉搏", "体温", "医院", "科室", "病区", "床号", "标本类型", "临床诊断");

    private static final List<String> FINDING_METADATA =
            List.of(
                    "姓名", "性别", "年龄", "出生日期", "体检日期", "检查日期", "报告日期",
                    "打印日期", "送检日期", "采样日期", "报告时间", "咨询电话", "联系电话",
                    "手机号码", "手机号", "身份证", "报告编号", "报告号", "体检编号", "住院号",
                    "门诊号", "床位号", "床号", "病区", "科室", "条码号", "检查号", "检验号",
                    "样本号", "申请单号", "仪器型号", "设备编号");

    private LabIndicatorVisibility() {}

    public static boolean isVisible(String name) {
        String compact = normalize(name);
        if (compact.isEmpty() || EXACT_METADATA.contains(compact)) {
            return false;
        }
        if (CONTAINS_METADATA.stream().anyMatch(compact::contains)) {
            return false;
        }
        return PROFILE_PREFIXES.stream().noneMatch(compact::startsWith);
    }

    /**
     * Findings may contain valuable non-numeric physical examination results, so this filter is
     * intentionally narrower than {@link #isVisible(String)} and removes administrative fields
     * only.
     */
    public static boolean isFindingVisible(String item) {
        String compact = normalize(item);
        return !compact.isEmpty()
                && FINDING_METADATA.stream().noneMatch(compact::contains);
    }

    /**
     * OCR may split one administrative row across the item and result columns, for example
     * {@code 姓 | 名 张某 性别 男 年龄 60 岁 检查日期 ...}. Keep a single incidental metadata
     * word in a legitimate medical sentence, but reject rows that reconstruct at least two
     * administrative labels when both columns are joined.
     */
    public static boolean isFindingVisible(String item, String result) {
        if (!isFindingVisible(item)) {
            return false;
        }
        String combined = normalize(item) + normalize(result);
        long metadataMarkers =
                FINDING_METADATA.stream().filter(combined::contains).distinct().count();
        return metadataMarkers < 2;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s:：|]+", "").toLowerCase(Locale.ROOT);
    }
}
