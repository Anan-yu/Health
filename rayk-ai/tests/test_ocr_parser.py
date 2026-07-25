from decimal import Decimal

from app.ocr.service import IndicatorRowParser, OcrQualityValidator
from app.schemas.indicator import IndicatorInput


def test_parser_extracts_known_indicator_and_reference_range() -> None:
    indicators = IndicatorRowParser().parse(["空腹血糖 6.20 mmol/L 3.90-6.10"])

    assert len(indicators) == 1
    assert indicators[0].code == "fasting_glucose"
    assert indicators[0].value == Decimal("6.20")
    assert indicators[0].reference_low == Decimal("3.90")
    assert indicators[0].reference_high == Decimal("6.10")


def test_parser_supports_english_aliases() -> None:
    indicators = IndicatorRowParser().parse(["HbA1c 5.8 % 4.0~6.0", "LDL-C 3.4 mmol/L 0-3.37"])

    assert [item.code for item in indicators] == ["hba1c", "ldl"]


def test_parser_recognizes_apolipoprotein_a1() -> None:
    indicators = IndicatorRowParser().parse(["载脂蛋白A1 1.23 g/L 1.00-1.60"])

    assert len(indicators) == 1
    assert indicators[0].code == "apoa1"


def test_parser_prefers_specific_alias_over_broad_alias() -> None:
    indicators = IndicatorRowParser().parse(
        [
            "低密度脂蛋白胆固醇 3.6 mmol/L 0.0-3.4",
            "平均红细胞血红蛋白量 26 pg 27-34",
            "粪便钙卫蛋白 62 μg/g 0-50",
        ]
    )

    assert [item.code for item in indicators] == ["ldl", "mch", "calprotectin"]


def test_parser_prefers_specific_alias_for_separate_table_cells() -> None:
    indicators = IndicatorRowParser().parse(
        [
            "高密度脂蛋白胆固醇",
            "1.2",
            "mmol/L",
            "1.0-2.0",
            "平均红细胞体积",
            "82",
            "fL",
            "80-100",
        ]
    )

    assert [item.code for item in indicators] == ["hdl", "mcv"]


def test_parser_recognizes_common_biochemistry_report_aliases() -> None:
    indicators = IndicatorRowParser().parse(
        [
            "钾离子 4.60 mmol/L 3.5-5.5",
            "钠离子 140 mmol/L 137-147",
            "钙离子 2.39 mmol/L 2.08-2.60",
            "氯离子 108 mmol/L 99-110",
            "碳酸氢根 31.4 mmol/L 20-30",
            "谷酰转肽酶 16 U/L 10-60",
        ]
    )

    assert [item.code for item in indicators] == [
        "potassium",
        "sodium",
        "calcium",
        "chloride",
        "bicarbonate",
        "ggt",
    ]


def test_parser_recovers_common_ocr_character_errors() -> None:
    indicators = IndicatorRowParser().parse(
        [
            "呷离子 4.60 mmol/L 3.5-5.5",
            "内离子 140 mmol/L 137-147",
            "炭酸氢根 31.4 mmol/L 20-30",
            "萄糖 4.92 mmol/L 3.91-6.10",
            "酯蛋白A1 1.11 g/L 0.85-1.69",
            "素氮 2.84 mmol/L 2.50-7.10",
            "酐 75.4 μmol/L 44-110",
        ]
    )

    assert [item.code for item in indicators] == [
        "potassium",
        "sodium",
        "bicarbonate",
        "fasting_glucose",
        "apoa1",
        "urea",
        "creatinine",
    ]


def test_parser_covers_common_liver_and_nutrition_panel() -> None:
    indicators = IndicatorRowParser().parse(
        [
            "总蛋白 70.5 g/L 65.0-85.0",
            "球蛋白 21.6 g/L 20.0-35.0",
            "碱性磷酸酶 53.8 U/L 45.0-125.0",
            "乳酸脱氢酶 132.9 U/L 60.0-240.0",
            "淀粉酶 53 U/L 20-110",
            "清磷 1.39 mmol/L 0.80-1.60",
        ]
    )

    assert [item.code for item in indicators] == [
        "total_protein",
        "globulin",
        "alkaline_phosphatase",
        "lactate_dehydrogenase",
        "amylase",
        "phosphorus",
    ]


def test_parser_recovers_two_column_blood_count_report_with_row_numbers() -> None:
    """Regression sample: two table columns are interleaved in PaddleOCR reading order."""
    lines = [
        "白细胞计数",
        "5.18",
        "4.00-12.00×10^9/L",
        "21",
        "血小板压积",
        "0.16",
        "0.07-0.35",
        "%",
        "2",
        "淋巴细胞百分比",
        "23.5",
        "20.0-40.0%",
        "22",
        "血小板分布宽度",
        "16.4",
        "0.0-20.0",
        "%",
        "3",
        "中性粒细胞百分比",
        "69.2",
        "50.0-75.0 %",
        "23",
        "有核红细胞：白细胞",
        "0.00",
        "%",
        "4 单核细胞百分比",
        "6.1",
        "4.0-16.0",
        "%",
        "24",
        "超敏C反应蛋白",
        "6.71",
        "0.00-8.00 mg/L",
        "5 嗜酸性粒细胞百分比",
        "1.0",
        "1.0-6.0",
        "%",
        "6 嗜碱性粒细胞百分比",
        "0.2",
        "0.0-1.0",
        "%",
        "7 淋巴细胞绝对值",
        "1.22",
        "0.70-4.90",
        "×10^9/L",
        "8 中性粒细胞绝对值",
        "3.58",
        "1.50-7.80",
        "×10^9/L",
        "9 单核细胞绝对值",
        "0.32",
        "0.10-1.50",
        "×10^9/L",
        "10 嗜酸性粒细胞绝对值",
        "0.05",
        "0.05-0.50",
        "×10^9/L",
        "11 嗜碱性粒细胞绝对值",
        "0.01",
        "0.00-0.30",
        "×10^9/L",
        "12 红细胞计数",
        "4.21",
        "3.50-5.50",
        "×10^12/L",
        "13 血红蛋白",
        "118",
        "110-155",
        "g/L",
        "14 红细胞比积",
        "34.9",
        "31.0-44.0 %",
        "15 平均红细胞体积",
        "82.9",
        "75.0-92.0 fL",
        "16 平均血红蛋白量",
        "28.1",
        "26.0-31.0 pg",
        "17 平均血红蛋白浓度",
        "338",
        "315-365",
        "g/L",
        "18 红细胞分布宽度",
        "12.7",
        "0.0-18.0",
        "%",
        "19 血小板计数",
        "159",
        "100-400",
        "×10^9/L",
        "20 平均血小板体积",
        "10.0",
        "6.5-11.5",
        "fL",
    ]

    indicators = IndicatorRowParser().parse(lines)
    codes = {item.code for item in indicators}

    assert len(indicators) == 23
    assert codes == {
        "wbc",
        "plateletcrit",
        "lymphocyte_percentage",
        "pdw",
        "neutrophil_percentage",
        "monocyte_percentage",
        "hs_crp",
        "eosinophil_percentage",
        "basophil_percentage",
        "lymphocyte_count",
        "neutrophil_count",
        "monocyte_count",
        "eosinophil_count",
        "basophil_count",
        "rbc",
        "hemoglobin",
        "hematocrit",
        "mcv",
        "mch",
        "mchc",
        "rdw",
        "platelet_count",
        "mpv",
    }
    assert all(not item.name.isdigit() for item in indicators)


def test_layout_parser_accepts_unfamiliar_header_names_and_abnormal_arrows() -> None:
    lines = [
        "检测项目",
        "测定结果",
        "结果单位",
        "生物参考区间",
        "总胆红素",
        "↑22.7",
        "μmol/L",
        "3.0—22.0",
        "陌生营养指标",
        "18.5↓",
        "mg/L",
        "20.0~40.0",
    ]
    boxes = [
        (10, 10, 90, 30),
        (120, 10, 190, 30),
        (210, 10, 270, 30),
        (300, 10, 400, 30),
        (10, 55, 100, 75),
        (120, 55, 180, 75),
        (210, 55, 270, 75),
        (300, 55, 390, 75),
        (10, 95, 110, 115),
        (120, 95, 180, 115),
        (210, 95, 270, 115),
        (300, 95, 390, 115),
    ]

    indicators = IndicatorRowParser().parse_layout(lines, boxes)

    assert indicators[0].code == "total_bilirubin"
    assert (indicators[1].code or "").startswith("unrecognized_")
    assert indicators[0].value == Decimal("22.7")
    assert indicators[1].name == "陌生营养指标"
    assert indicators[1].value == Decimal("18.5")


def test_layout_parser_keeps_both_sides_of_a_repeated_table_together() -> None:
    lines = [
        "检验项目",
        "结果",
        "单位",
        "参考范围",
        "检验项目",
        "结果",
        "单位",
        "参考范围",
        "钾离子",
        "4.60",
        "mmol/L",
        "3.5-5.5",
        "谷丙转氨酶",
        "14.4",
        "U/L",
        "9.0-50.0",
    ]
    boxes = [
        (10, 10, 80, 30),
        (100, 10, 150, 30),
        (175, 10, 230, 30),
        (245, 10, 305, 30),
        (320, 10, 390, 30),
        (410, 10, 460, 30),
        (485, 10, 535, 30),
        (550, 10, 620, 30),
        (10, 55, 80, 75),
        (100, 55, 150, 75),
        (175, 55, 230, 75),
        (245, 55, 305, 75),
        (320, 55, 400, 75),
        (410, 55, 460, 75),
        (485, 55, 535, 75),
        (550, 55, 620, 75),
    ]

    indicators = IndicatorRowParser().parse_layout(lines, boxes)

    assert [item.code for item in indicators] == ["potassium", "alt"]
    assert indicators[0].reference_high == Decimal("5.5")
    assert indicators[1].reference_low == Decimal("9.0")


def test_parser_supports_one_sided_reference_ranges() -> None:
    indicators = IndicatorRowParser().parse(
        [
            "C反应蛋白 4.10 mg/L <5.00",
            "高密度脂蛋白胆固醇 1.20 mmol/L ≥1.00",
        ]
    )

    assert indicators[0].reference_low is None
    assert indicators[0].reference_high == Decimal("5.00")
    assert indicators[1].reference_low == Decimal("1.00")
    assert indicators[1].reference_high is None


def test_layout_parser_does_not_treat_demographics_as_indicators() -> None:
    lines = ["姓名", "王某", "年龄", "27", "岁", "20-30"]
    boxes = [
        (10, 10, 60, 30),
        (80, 10, 130, 30),
        (10, 50, 60, 70),
        (80, 50, 110, 70),
        (130, 50, 160, 70),
        (180, 50, 240, 70),
    ]

    assert IndicatorRowParser().parse_layout(lines, boxes) == []


def test_quality_validator_blocks_empty_and_invalid_results() -> None:
    validator = OcrQualityValidator()
    invalid = IndicatorInput(
        code="age",
        name="年龄",
        value=Decimal("27"),
        unit="岁",
        referenceLow=Decimal("20"),
        referenceHigh=Decimal("30"),
    )

    result = validator.validate([invalid], Decimal("0.98"), ["年龄", "27"])

    assert result.status == "RETRY_REQUIRED"
    assert result.indicators == []
    assert any("重新上传" in warning for warning in result.warnings)


def test_quality_validator_combines_text_and_structure_confidence() -> None:
    validator = OcrQualityValidator()
    valid = IndicatorInput(
        code="fasting_glucose",
        name="空腹血糖",
        value=Decimal("5.2"),
        unit="mmol/L",
        referenceLow=Decimal("3.9"),
        referenceHigh=Decimal("6.1"),
    )

    result = validator.validate([valid], Decimal("0.96"), ["空腹血糖 5.2 mmol/L 3.9-6.1"])

    assert result.status == "SUCCESS"
    assert result.confidence >= Decimal("0.90")


def test_quality_validator_rejects_shifted_laboratory_columns() -> None:
    validator = OcrQualityValidator()
    shifted = [
        IndicatorInput(
            code="phosphorus",
            name="血清磷",
            value=Decimal("60"),
            unit="mmol/L",
            referenceLow=Decimal("60"),
            referenceHigh=Decimal("240"),
        ),
        IndicatorInput(
            code="magnesium",
            name="血清镁",
            value=Decimal("8.8"),
            unit="mmol/L",
            referenceLow=Decimal("1"),
            referenceHigh=Decimal("6"),
        ),
        IndicatorInput(
            code="prealbumin",
            name="前白蛋白",
            value=Decimal("192.5"),
            unit="mg/L",
            referenceLow=Decimal("1.2"),
            referenceHigh=Decimal("4"),
        ),
    ]

    result = validator.validate(shifted, Decimal("0.98"), ["复杂双栏检验报告"])

    assert result.status == "RETRY_REQUIRED"
    assert result.indicators == []
    assert any("已拦截 3 项" in warning for warning in result.warnings)


def test_quality_validator_selects_safe_candidate_from_multiple_parsers() -> None:
    validator = OcrQualityValidator()
    shifted = IndicatorInput(
        code="bicarbonate",
        name="碳酸氢根",
        value=Decimal("19.5"),
        unit="mmol/L",
        referenceLow=Decimal("45"),
        referenceHigh=Decimal("125"),
    )
    correct = IndicatorInput(
        code="bicarbonate",
        name="碳酸氢根",
        value=Decimal("31.4"),
        unit="mmol/L",
        referenceLow=Decimal("20"),
        referenceHigh=Decimal("30"),
    )

    result = validator.validate(
        [shifted, correct],
        Decimal("0.96"),
        ["碳酸氢根 31.4 mmol/L 20-30"],
    )

    assert result.status == "SUCCESS"
    assert result.indicators == [correct]


def test_quality_validator_requires_retry_when_too_many_rows_are_unsafe() -> None:
    validator = OcrQualityValidator()
    valid_rows = [
        IndicatorInput(
            code=f"safe_{index}",
            name=f"安全指标{index}",
            value=Decimal("5"),
            unit="U/L",
            referenceLow=Decimal("1"),
            referenceHigh=Decimal("10"),
        )
        for index in range(4)
    ]
    shifted = IndicatorInput(
        code="phosphorus",
        name="血清磷",
        value=Decimal("60"),
        unit="mmol/L",
        referenceLow=Decimal("60"),
        referenceHigh=Decimal("240"),
    )

    result = validator.validate(
        [*valid_rows, shifted],
        Decimal("0.98"),
        ["含有一项错位数据的五项报告"],
    )

    assert result.status == "RETRY_REQUIRED"
    assert shifted not in result.indicators
