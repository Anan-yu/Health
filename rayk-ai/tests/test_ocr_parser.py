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


def test_parser_keeps_generic_numeric_rows_for_manual_review() -> None:
    indicators = IndicatorRowParser().parse(["载脂蛋白A1 1.23 g/L 1.00-1.60"])

    assert len(indicators) == 1
    assert indicators[0].code is not None
    assert indicators[0].code.startswith("unrecognized_")


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
