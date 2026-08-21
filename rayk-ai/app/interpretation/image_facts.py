"""Project validated multimodal report facts into the existing assessment context."""

from __future__ import annotations

import re
from decimal import Decimal, InvalidOperation

from app.schemas.assessment import VisionImageAnalysis
from app.schemas.indicator import IndicatorInput
from app.schemas.ocr import OcrFinding


_NUMBER = r"[-+]?(?:\d+(?:\.\d+)?|\.\d+)"
_NUMBER_RE = re.compile(rf"(?<![A-Za-z]){_NUMBER}(?![A-Za-z])")
_RANGE_RE = re.compile(
    rf"(?P<low>{_NUMBER})\s*(?:至|到|~|～|—|–|-{{1,2}})\s*(?P<high>{_NUMBER})"
)


# These aliases intentionally cover only indicators for which the rule engine has an
# explainable, conservative health-management rule. Unrecognised numeric findings still
# remain in the page facts and examination snapshot; they are not silently discarded.
_INDICATOR_ALIASES: tuple[tuple[tuple[str, ...], str, str, str], ...] = (
    (("高敏C反应蛋白", "HS-CRP", "HSCRP"), "hs_crp", "高敏C反应蛋白", "mg/L"),
    (("空腹血糖", "葡萄糖", "GLU", "GLUCOSE"), "fasting_glucose", "空腹血糖", "mmol/L"),
    (("空腹胰岛素",), "fasting_insulin", "空腹胰岛素", "μIU/mL"),
    (("糖化血红蛋白", "HBA1C", "HbA1c"), "hba1c", "糖化血红蛋白", "%"),
    (("总胆固醇", "TC", "TOTALCHOLESTEROL"), "total_cholesterol", "总胆固醇", "mmol/L"),
    (("低密度脂蛋白胆固醇", "低密度脂蛋白", "LDL-C", "LDL"), "ldl", "低密度脂蛋白", "mmol/L"),
    (("高密度脂蛋白胆固醇", "高密度脂蛋白", "HDL-C", "HDL"), "hdl", "高密度脂蛋白", "mmol/L"),
    (("甘油三酯", "TG", "TRIGLYCERIDE"), "triglyceride", "甘油三酯", "mmol/L"),
    (("载脂蛋白B", "载脂蛋白B", "APOB"), "apob", "载脂蛋白B", "g/L"),
    (("脂蛋白(a)", "脂蛋白A", "LPA"), "lpa", "脂蛋白(a)", "nmol/L"),
    (("C反应蛋白", "CRP"), "crp", "C反应蛋白", "mg/L"),
    (("红细胞沉降率", "血沉", "ESR"), "esr", "红细胞沉降率", "mm/h"),
    (("同型半胱氨酸", "Hcy", "HCY"), "homocysteine", "同型半胱氨酸", "umol/L"),
    (("铁蛋白", "FERRITIN"), "ferritin", "铁蛋白", "ng/mL"),
    (("白细胞", "白细胞计数", "WBC"), "wbc", "白细胞计数", "10^9/L"),
    (("血红蛋白", "血色素", "HGB", "HB"), "hemoglobin", "血红蛋白", "g/L"),
    (("红细胞计数", "红细胞", "RBC"), "rbc", "红细胞计数", "10^12/L"),
    (("平均红细胞体积", "MCV"), "mcv", "平均红细胞体积", "fL"),
    (("平均红细胞血红蛋白量", "MCH"), "mch", "平均红细胞血红蛋白量", "pg"),
    (("维生素B12", "VB12", "VITAMINB12"), "vitamin_b12", "维生素B12", "pg/mL"),
    (("叶酸", "FOLATE"), "folate", "叶酸", "ng/mL"),
    (("维生素D", "25-羟维生素D", "25-OH-D", "25OHD"), "vitamin_d", "维生素D", "ng/mL"),
    (("锌", "ZINC"), "zinc", "锌", "ug/dL"),
    (("镁", "MAGNESIUM"), "magnesium", "镁", "mmol/L"),
    (("钙", "血钙", "CALCIUM"), "calcium", "钙", "mmol/L"),
    (("白蛋白", "血清白蛋白", "ALB", "ALBUMIN"), "albumin", "白蛋白", "g/L"),
    (("丙氨酸氨基转移酶", "谷丙转氨酶", "ALT"), "alt", "丙氨酸氨基转移酶", "U/L"),
    (("天门冬氨酸氨基转移酶", "谷草转氨酶", "AST"), "ast", "天门冬氨酸氨基转移酶", "U/L"),
    (("γ-谷氨酰转移酶", "γ谷氨酰转移酶", "谷氨酰转肽酶", "GGT"), "ggt", "γ-谷氨酰转移酶", "U/L"),
    (("总胆红素", "TBIL", "TOTALBILIRUBIN"), "total_bilirubin", "总胆红素", "umol/L"),
    (("直接胆红素", "DBIL", "DIRECTBILIRUBIN"), "direct_bilirubin", "直接胆红素", "umol/L"),
    (("肌酐", "CREA", "CREATININE"), "creatinine", "肌酐", "umol/L"),
    (("估算肾小球滤过率", "肾小球滤过率", "EGFR", "GFR"), "egfr", "估算肾小球滤过率", "mL/min/1.73m2"),
    (("尿素", "尿素氮", "BUN", "UREA"), "urea", "尿素", "mmol/L"),
    (("尿酸", "UA", "URICACID"), "uric_acid", "尿酸", "umol/L"),
    (("钠", "血钠", "NA", "SODIUM"), "sodium", "钠", "mmol/L"),
    (("钾", "血钾", "K", "POTASSIUM"), "potassium", "钾", "mmol/L"),
    (("氯", "血氯", "CL", "CHLORIDE"), "chloride", "氯", "mmol/L"),
    (("碳酸氢根", "二氧化碳结合力", "HCO3", "BICARBONATE"), "bicarbonate", "碳酸氢根", "mmol/L"),
    (("促甲状腺激素", "TSH"), "tsh", "促甲状腺激素", "mIU/L"),
    (("游离三碘甲状腺原氨酸", "游离T3", "FT3"), "ft3", "游离T3", "pmol/L"),
    (("游离甲状腺素", "游离T4", "FT4"), "ft4", "游离T4", "pmol/L"),
)


def _compact(value: str) -> str:
    return re.sub(r"[\s:：()（）\[\]【】/\\]", "", value.strip()).upper()


def _definition(item: str) -> tuple[str, str, str] | None:
    normalized = _compact(item)
    for aliases, code, name, unit in _INDICATOR_ALIASES:
        if any(_compact(alias) in normalized for alias in aliases):
            return code, name, unit
    return None


def _decimal(value: str) -> Decimal | None:
    try:
        return Decimal(value.replace(",", "").strip())
    except (InvalidOperation, ValueError):
        return None


def _result_value(value: str) -> Decimal | None:
    matches = _NUMBER_RE.findall(value.replace(",", ""))
    if len(matches) != 1:
        return None
    return _decimal(matches[0])


def _reference_range(value: str | None) -> tuple[Decimal | None, Decimal | None]:
    if not value:
        return None, None
    match = _RANGE_RE.search(value.replace(",", ""))
    if match:
        return _decimal(match.group("low")), _decimal(match.group("high"))
    numbers = _NUMBER_RE.findall(value.replace(",", ""))
    if len(numbers) == 1:
        number = _decimal(numbers[0])
        if number is None:
            return None, None
        if any(marker in value for marker in ("<", "≤", "不高于", "不超过")):
            return None, number
        if any(marker in value for marker in (">", "≥", "不低于", "不少于")):
            return number, None
    return None, None


def project_image_analysis(
    analysis: VisionImageAnalysis,
) -> tuple[list[IndicatorInput], list[OcrFinding]]:
    """Return safe numeric indicators and qualitative findings without dropping page facts."""

    indicators: list[IndicatorInput] = []
    seen_codes: set[str] = set()
    findings: list[OcrFinding] = []
    for page in analysis.pages:
        section = page.findings[0].category.strip() if page.findings else ""
        section = section or f"体检报告第{page.page}页"
        if page.page_summary.strip():
            findings.append(
                OcrFinding(
                    section=section,
                    item="页面小结",
                    result=page.page_summary.strip(),
                )
            )
        for source in page.findings:
            item = source.item.strip()
            result = source.result.strip()
            if not item or not result:
                continue
            section_name = source.category.strip() or section
            result_text = result
            if source.unit:
                result_text += f" {source.unit.strip()}"
            if source.reference_range:
                result_text += f"（参考范围：{source.reference_range.strip()}）"
            if source.abnormal_flag:
                result_text += f"（异常标识：{source.abnormal_flag.strip()}）"
            findings.append(OcrFinding(section=section_name, item=item, result=result_text))
            if source.conclusion and source.conclusion.strip():
                findings.append(
                    OcrFinding(
                        section=section_name,
                        item="检查小结",
                        result=source.conclusion.strip(),
                    )
                )

            definition = _definition(item)
            value = _result_value(result)
            if definition is None or value is None:
                continue
            code, name, default_unit = definition
            if code in seen_codes:
                continue
            low, high = _reference_range(source.reference_range)
            indicators.append(
                IndicatorInput(
                    code=code,
                    name=name,
                    value=value,
                    unit=source.unit.strip() or default_unit,
                    reference_low=low,
                    reference_high=high,
                )
            )
            seen_codes.add(code)
    return indicators, findings
