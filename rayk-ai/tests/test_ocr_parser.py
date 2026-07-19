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
