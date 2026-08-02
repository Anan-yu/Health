package com.rayk.health.laboratory.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LabIndicatorVisibilityTest {
    @Test
    void rejectsIdentityDatesContactAndProfileMetadata() {
        assertThat(LabIndicatorVisibility.isVisible("体检日期")).isFalse();
        assertThat(LabIndicatorVisibility.isVisible("咨询电话")).isFalse();
        assertThat(LabIndicatorVisibility.isVisible("身高 160 cm")).isFalse();
        assertThat(LabIndicatorVisibility.isVisible("姓名 高建刚 男 60 岁")).isFalse();
        assertThat(LabIndicatorVisibility.isVisible("打印日期 2026年08月01日 09")).isFalse();
        assertThat(LabIndicatorVisibility.isVisible("床位号")).isFalse();
        assertThat(LabIndicatorVisibility.isVisible("检查号 / 仪器型号")).isFalse();
    }

    @Test
    void keepsRealLaboratoryIndicators() {
        assertThat(LabIndicatorVisibility.isVisible("总胆固醇")).isTrue();
        assertThat(LabIndicatorVisibility.isVisible("血小板压积")).isTrue();
        assertThat(LabIndicatorVisibility.isVisible("低密度脂蛋白胆固醇")).isTrue();
    }

    @Test
    void removesAdministrativeRowsFromNonNumericFindings() {
        assertThat(LabIndicatorVisibility.isFindingVisible("门诊号")).isFalse();
        assertThat(LabIndicatorVisibility.isFindingVisible("床位号")).isFalse();
        assertThat(LabIndicatorVisibility.isFindingVisible("检查号 / 仪器型号")).isFalse();
        assertThat(LabIndicatorVisibility.isFindingVisible("胆囊")).isTrue();
        assertThat(LabIndicatorVisibility.isFindingVisible("检查小结")).isTrue();
    }

    @Test
    void removesAdministrativeRowsSplitAcrossFindingColumns() {
        assertThat(
                        LabIndicatorVisibility.isFindingVisible(
                                "姓", "名 张某 性 别 男 年 龄 60 岁 检查日期 2026年07月25日"))
                .isFalse();
        assertThat(
                        LabIndicatorVisibility.isFindingVisible(
                                "检查所见", "甲状腺形态正常，建议结合年龄变化定期随访"))
                .isTrue();
        assertThat(LabIndicatorVisibility.isFindingVisible("检查小结", "未见明显异常"))
                .isTrue();
    }
}
