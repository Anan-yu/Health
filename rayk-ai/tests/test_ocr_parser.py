from decimal import Decimal

from app.ocr.service import IndicatorRowParser


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
