import hashlib
import os
import re
import tempfile
from abc import ABC, abstractmethod
from collections.abc import Iterable
from dataclasses import dataclass
from decimal import Decimal
from functools import lru_cache
from pathlib import Path
from statistics import median
from typing import Any

import httpx
from PIL import Image, ImageOps

from app.schemas.indicator import IndicatorInput
from app.schemas.ocr import OcrRecognizeData, OcrRecognizeRequest

NUMBER_PATTERN = re.compile(r"[-+]?\d+(?:[.,]\d+)?")
ROW_NUMBER_PATTERN = re.compile(r"^\s*\d{1,3}\s*")


@dataclass(frozen=True)
class LayoutToken:
    text: str
    box: tuple[float, float, float, float]

    @property
    def left(self) -> float:
        return self.box[0]

    @property
    def right(self) -> float:
        return self.box[2]

    @property
    def center_x(self) -> float:
        return (self.left + self.right) / 2

    @property
    def center_y(self) -> float:
        return (self.box[1] + self.box[3]) / 2

    @property
    def height(self) -> float:
        return self.box[3] - self.box[1]


class OcrService(ABC):
    @abstractmethod
    def recognize(self, request: OcrRecognizeRequest) -> OcrRecognizeData: ...


class IndicatorRowParser:
    INDICATORS: tuple[tuple[str, str, str, tuple[str, ...]], ...] = (
        ("fasting_glucose", "空腹血糖", "mmol/L", ("空腹血糖", "葡萄糖", "GLU")),
        ("fasting_insulin", "空腹胰岛素", "μIU/mL", ("空腹胰岛素", "FINS")),
        ("hba1c", "糖化血红蛋白", "%", ("糖化血红蛋白", "糖化血色素", "HbA1c")),
        ("hs_crp", "高敏C反应蛋白", "mg/L", ("超敏C反应蛋白", "高敏C反应蛋白", "hs-CRP")),
        ("crp", "C反应蛋白", "mg/L", ("C反应蛋白", "CRP")),
        ("esr", "红细胞沉降率", "mm/h", ("红细胞沉降率", "血沉", "ESR")),
        ("homocysteine", "同型半胱氨酸", "μmol/L", ("同型半胱氨酸", "HCY")),
        ("ferritin", "铁蛋白", "ng/mL", ("铁蛋白", "FER")),
        ("wbc", "白细胞计数", "10^9/L", ("白细胞计数", "白细胞", "WBC")),
        (
            "lymphocyte_percentage",
            "淋巴细胞百分比",
            "%",
            ("淋巴细胞百分比", "淋巴细胞比率", "LYM%"),
        ),
        (
            "neutrophil_percentage",
            "中性粒细胞百分比",
            "%",
            ("中性粒细胞百分比", "中性粒细胞比率", "NEUT%"),
        ),
        (
            "monocyte_percentage",
            "单核细胞百分比",
            "%",
            ("单核细胞百分比", "单核细胞比率", "MONO%"),
        ),
        (
            "eosinophil_percentage",
            "嗜酸性粒细胞百分比",
            "%",
            ("嗜酸性粒细胞百分比", "嗜酸性粒细胞比率", "EOS%"),
        ),
        (
            "basophil_percentage",
            "嗜碱性粒细胞百分比",
            "%",
            ("嗜碱性粒细胞百分比", "嗜碱性粒细胞比率", "BASO%"),
        ),
        (
            "lymphocyte_count",
            "淋巴细胞绝对值",
            "10^9/L",
            ("淋巴细胞绝对值", "淋巴细胞计数", "LYM#"),
        ),
        (
            "neutrophil_count",
            "中性粒细胞绝对值",
            "10^9/L",
            ("中性粒细胞绝对值", "中性粒细胞计数", "NEUT#"),
        ),
        (
            "monocyte_count",
            "单核细胞绝对值",
            "10^9/L",
            ("单核细胞绝对值", "单核细胞计数", "MONO#"),
        ),
        (
            "eosinophil_count",
            "嗜酸性粒细胞绝对值",
            "10^9/L",
            ("嗜酸性粒细胞绝对值", "嗜酸性粒细胞计数", "EOS#"),
        ),
        (
            "basophil_count",
            "嗜碱性粒细胞绝对值",
            "10^9/L",
            ("嗜碱性粒细胞绝对值", "嗜碱性粒细胞计数", "BASO#"),
        ),
        ("total_cholesterol", "总胆固醇", "mmol/L", ("总胆固醇", "胆固醇", "TC")),
        ("triglyceride", "甘油三酯", "mmol/L", ("甘油三酯", "TG")),
        ("hdl", "高密度脂蛋白胆固醇", "mmol/L", ("高密度脂蛋白胆固醇", "HDL-C", "HDL")),
        ("ldl", "低密度脂蛋白胆固醇", "mmol/L", ("低密度脂蛋白胆固醇", "LDL-C", "LDL")),
        ("apob", "载脂蛋白B", "g/L", ("载脂蛋白B", "载酯蛋白-B", "ApoB")),
        ("apoa1", "载脂蛋白A1", "g/L", ("载脂蛋白A1", "载酯蛋白A1", "ApoA1")),
        ("lpa", "脂蛋白(a)", "nmol/L", ("脂蛋白(a)", "Lp(a)")),
        ("alt", "丙氨酸氨基转移酶", "U/L", ("丙氨酸氨基转移酶", "谷丙转氨酶", "ALT")),
        ("ast", "天门冬氨酸氨基转移酶", "U/L", ("天门冬氨酸氨基转移酶", "谷草转氨酶", "AST")),
        (
            "ggt",
            "γ-谷氨酰转移酶",
            "U/L",
            ("γ-谷氨酰转移酶", "谷氨酰转肽酶", "谷酰转肽酶", "GGT"),
        ),
        ("total_bilirubin", "总胆红素", "μmol/L", ("总胆红素", "TBIL")),
        ("direct_bilirubin", "直接胆红素", "μmol/L", ("直接胆红素", "DBIL")),
        ("indirect_bilirubin", "间接胆红素", "μmol/L", ("间接胆红素", "IBIL")),
        ("albumin", "白蛋白", "g/L", ("白蛋白", "ALB")),
        ("prealbumin", "前白蛋白", "mg/L", ("前白蛋白", "PA")),
        ("albumin_globulin_ratio", "白球比", "ratio", ("白球比", "A/G")),
        ("creatinine", "肌酐", "μmol/L", ("血肌酐", "肌酐", "CREA", "Cr")),
        ("egfr", "估算肾小球滤过率", "mL/min/1.73m2", ("估算肾小球滤过率", "eGFR")),
        ("urea", "尿素", "mmol/L", ("尿素氮", "尿素", "UREA", "BUN")),
        ("uric_acid", "尿酸", "μmol/L", ("尿酸", "UA")),
        ("sodium", "钠", "mmol/L", ("血清钠", "血钠", "钠离子", "Na+")),
        ("potassium", "钾", "mmol/L", ("血清钾", "血钾", "钾离子", "K+")),
        ("hemoglobin", "血红蛋白", "g/L", ("血红蛋白", "HGB")),
        ("rbc", "红细胞计数", "10^12/L", ("红细胞计数", "红细胞", "RBC")),
        ("mcv", "平均红细胞体积", "fL", ("平均红细胞体积", "MCV")),
        (
            "hematocrit",
            "红细胞比积",
            "%",
            ("红细胞比积", "红细胞压积", "HCT"),
        ),
        (
            "mch",
            "平均红细胞血红蛋白量",
            "pg",
            ("平均红细胞血红蛋白量", "平均血红蛋白量", "MCH"),
        ),
        (
            "mchc",
            "平均红细胞血红蛋白浓度",
            "g/L",
            ("平均红细胞血红蛋白浓度", "平均血红蛋白浓度", "MCHC"),
        ),
        (
            "rdw",
            "红细胞分布宽度",
            "%",
            ("红细胞分布宽度", "RDW-CV", "RDW"),
        ),
        (
            "platelet_count",
            "血小板计数",
            "10^9/L",
            ("血小板计数", "血小板", "PLT"),
        ),
        (
            "mpv",
            "平均血小板体积",
            "fL",
            ("平均血小板体积", "MPV"),
        ),
        (
            "plateletcrit",
            "血小板压积",
            "%",
            ("血小板压积", "PCT"),
        ),
        (
            "pdw",
            "血小板分布宽度",
            "%",
            ("血小板分布宽度", "PDW"),
        ),
        ("tsh", "促甲状腺激素", "mIU/L", ("促甲状腺激素", "TSH")),
        ("ft3", "游离三碘甲状腺原氨酸", "pmol/L", ("游离三碘甲状腺原氨酸", "游离T3", "FT3")),
        ("ft4", "游离甲状腺素", "pmol/L", ("游离甲状腺素", "游离T4", "FT4")),
        ("tpo_ab", "甲状腺过氧化物酶抗体", "IU/mL", ("甲状腺过氧化物酶抗体", "TPOAb")),
        ("tg_ab", "甲状腺球蛋白抗体", "IU/mL", ("甲状腺球蛋白抗体", "TGAb")),
        ("estradiol", "雌二醇", "pg/mL", ("雌二醇", "E2")),
        ("progesterone", "孕酮", "ng/mL", ("孕酮",)),
        ("testosterone", "睾酮", "ng/mL", ("睾酮", "TESTO")),
        ("shbg", "性激素结合球蛋白", "nmol/L", ("性激素结合球蛋白", "SHBG")),
        ("lh", "黄体生成素", "IU/L", ("黄体生成素", "LH")),
        ("fsh", "卵泡刺激素", "IU/L", ("卵泡刺激素", "FSH")),
        ("cortisol_am", "晨间皮质醇", "μg/dL", ("晨间皮质醇", "上午皮质醇")),
        ("cortisol_pm", "晚间皮质醇", "μg/dL", ("晚间皮质醇", "下午皮质醇")),
        ("dhea_s", "脱氢表雄酮硫酸酯", "μg/dL", ("脱氢表雄酮硫酸酯", "DHEA-S")),
        ("sleep_hours", "平均睡眠时长", "h", ("平均睡眠时长", "Sleep hours")),
        ("hrv", "心率变异性", "ms", ("心率变异性", "HRV")),
        ("vitamin_d", "25羟维生素D", "ng/mL", ("25羟维生素D", "25-OH-D", "维生素D")),
        ("vitamin_b12", "维生素B12", "pg/mL", ("维生素B12", "VB12")),
        ("folate", "叶酸", "ng/mL", ("叶酸", "FOL")),
        ("zinc", "锌", "μg/dL", ("血清锌", "锌")),
        ("magnesium", "镁", "mmol/L", ("血清镁", "镁")),
        ("calcium", "钙", "mmol/L", ("血清钙", "钙离子", "钙")),
        ("chloride", "氯", "mmol/L", ("血清氯", "血氯", "氯离子", "Cl-")),
        ("bicarbonate", "碳酸氢根", "mmol/L", ("碳酸氢根", "HCO3-")),
        ("zonulin", "连蛋白", "ng/mL", ("连蛋白", "Zonulin")),
        ("calprotectin", "粪便钙卫蛋白", "μg/g", ("粪便钙卫蛋白", "钙卫蛋白")),
        ("occult_blood", "便潜血", "index", ("便潜血", "潜血", "OB")),
        ("stool_ph", "粪便pH", "index", ("粪便pH", "Stool pH")),
        (
            "digestive_symptom_score",
            "消化症状评分",
            "index",
            ("消化症状评分", "Digestive symptom score"),
        ),
        ("blood_lead", "血铅", "μg/L", ("血铅", "Pb")),
        ("blood_mercury", "血汞", "μg/L", ("血汞", "Hg")),
        ("cadmium", "镉", "μg/L", ("血镉", "镉")),
        ("arsenic", "砷", "μg/L", ("血砷", "砷")),
        (
            "heavy_metal_panel",
            "重金属组合异常指数",
            "index",
            ("重金属组合异常指数", "Heavy metal panel"),
        ),
    )

    def parse(self, lines: Iterable[str]) -> list[IndicatorInput]:
        parsed: dict[str, IndicatorInput] = {}
        normalized_lines = [self._normalize(line) for line in lines if line.strip()]
        for index, line in enumerate(normalized_lines):
            known = self._parse_known(line)
            if known is None:
                known = self._parse_cells(normalized_lines, index)
            if known is not None:
                parsed[known.code or known.name] = known
                continue
            generic = self._parse_generic(line)
            if generic is not None:
                parsed[generic.code or generic.name] = generic
        return list(parsed.values())

    def parse_layout(
        self, lines: Iterable[str], boxes: Iterable[tuple[float, float, float, float]]
    ) -> list[IndicatorInput]:
        """Reconstruct a laboratory table from OCR text boxes.

        PaddleOCR emits text in detection order, which interleaves left and right table columns.
        Grouping the boxes into visual rows first keeps a result, unit and reference interval
        attached to the correct analyte.
        """
        tokens: list[LayoutToken] = []
        for text, box in zip(lines, boxes, strict=False):
            normalized_box = self._box(box)
            if str(text).strip() and normalized_box is not None:
                tokens.append(LayoutToken(self._normalize(str(text)), normalized_box))
        if not tokens:
            return []
        table_header = [
            token for token in tokens if token.text in {"检验项目", "结果", "单位", "参考范围"}
        ]
        if len(table_header) < 4:
            return []
        first_row_y = max(token.center_y for token in table_header) + 18
        table_tokens = [token for token in tokens if token.center_y >= first_row_y]
        if not table_tokens:
            return []
        page_midpoint = max(token.right for token in tokens) / 2
        parsed: dict[str, IndicatorInput] = {}
        for column in (
            [token for token in table_tokens if token.center_x < page_midpoint],
            [token for token in table_tokens if token.center_x >= page_midpoint],
        ):
            for row in self._group_rows(column):
                item = self._parse_layout_row(row)
                if item is not None:
                    parsed[item.code or item.name] = item
        return list(parsed.values())

    def _parse_layout_row(self, row: list["LayoutToken"]) -> IndicatorInput | None:
        ordered = sorted(row, key=lambda token: token.left)
        if len(ordered) < 3:
            return None
        name_index = next(
            (index for index, token in enumerate(ordered) if self._clean_name(token.text)),
            None,
        )
        if name_index is None:
            return None
        name_token = self._clean_name(ordered[name_index].text)
        matched = self._matched_indicator(name_token, exact=True)
        value_index = next(
            (
                index
                for index, token in enumerate(ordered[name_index + 1 :], start=name_index + 1)
                if NUMBER_PATTERN.fullmatch(token.text)
            ),
            None,
        )
        if value_index is None:
            return None
        value_token = ordered[value_index].text
        reference_text = " ".join(token.text for token in ordered[value_index + 1 :])
        reference_low, reference_high = self._reference_values(
            reference_text,
            [self._decimal(value) for value in NUMBER_PATTERN.findall(reference_text)],
            value_included=False,
        )
        if value_token is None or reference_low is None or reference_high is None:
            return None
        if matched is not None:
            code, standard_name, standard_unit, _ = matched
            return IndicatorInput(
                code=code,
                name=standard_name,
                value=self._decimal(value_token),
                unit=self._unit(" ".join(token.text for token in ordered), standard_unit),
                referenceLow=reference_low,
                referenceHigh=reference_high,
            )
        unit = self._unit(" ".join(token.text for token in ordered[1:]), "")
        if not name_token or not unit:
            return None
        return IndicatorInput(
            code="unrecognized_" + hashlib.sha1(name_token.encode()).hexdigest()[:10],
            name=name_token,
            value=self._decimal(value_token),
            unit=unit,
            referenceLow=reference_low,
            referenceHigh=reference_high,
        )

    def _group_rows(self, tokens: list["LayoutToken"]) -> list[list["LayoutToken"]]:
        if not tokens:
            return []
        tolerance = max(22.0, median(token.height for token in tokens) * 0.62)
        rows: list[list[LayoutToken]] = []
        centers: list[float] = []
        for token in sorted(tokens, key=lambda item: item.center_y):
            if centers and abs(token.center_y - centers[-1]) <= tolerance:
                rows[-1].append(token)
                centers[-1] = sum(item.center_y for item in rows[-1]) / len(rows[-1])
            else:
                rows.append([token])
                centers.append(token.center_y)
        return rows

    def _box(self, value: Any) -> tuple[float, float, float, float] | None:
        if hasattr(value, "tolist"):
            value = value.tolist()
        values = list(value) if isinstance(value, list | tuple) else []
        if len(values) != 4:
            return None
        try:
            left, top, right, bottom = (float(item) for item in values)
        except (TypeError, ValueError):
            return None
        return left, top, right, bottom

    def _parse_cells(self, lines: list[str], index: int) -> IndicatorInput | None:
        """Parse OCR tables that emit name, value, unit and range as separate cells.

        Hospital reports vary between ``name/value/unit/range`` and
        ``name/value/range-with-unit``. Row numbers may also be attached to the name.
        """
        if index + 2 >= len(lines):
            return None
        name = self._clean_name(lines[index])
        if not name:
            return None
        value_text = lines[index + 1].strip()
        if NUMBER_PATTERN.fullmatch(value_text) is None:
            return None
        following: list[str] = []
        for cell in lines[index + 2 : index + 6]:
            cleaned_cell = self._clean_name(cell)
            if (
                following
                and cleaned_cell
                and (
                    self._matched_indicator(cleaned_cell) is not None
                    or re.search(r"[\u4e00-\u9fa5]", cleaned_cell)
                )
            ):
                break
            following.append(cell)
        range_text = next(
            (
                cell
                for cell in following
                if re.search(
                    r"[-+]?\d+(?:[.,]\d+)?\s*(?:-|~|—|至)\s*[-+]?\d+(?:[.,]\d+)?",
                    cell,
                )
            ),
            "",
        )
        if not range_text:
            return None
        range_numbers = [self._decimal(value) for value in NUMBER_PATTERN.findall(range_text)]
        reference_low, reference_high = self._reference_values(
            range_text, range_numbers, value_included=False
        )
        if reference_low is None or reference_high is None:
            return None

        matched = self._matched_indicator(name)
        combined_tail = " ".join(following)
        if matched is not None:
            code, standard_name, standard_unit, _ = matched
            return IndicatorInput(
                code=code,
                name=standard_name,
                value=self._decimal(value_text),
                unit=self._unit(combined_tail, standard_unit),
                referenceLow=reference_low,
                referenceHigh=reference_high,
            )
        if re.search(r"[\u4e00-\u9fa5A-Za-z]", name) is None:
            return None
        return IndicatorInput(
            code="unrecognized_" + hashlib.sha1(name.encode()).hexdigest()[:10],
            name=name,
            value=self._decimal(value_text),
            unit=self._unit(combined_tail, "index"),
            referenceLow=reference_low,
            referenceHigh=reference_high,
        )

    def _parse_known(self, line: str) -> IndicatorInput | None:
        lowered = line.casefold()
        matched = self._matched_indicator(line)
        if matched is not None:
            code, standard_name, standard_unit, alias = matched
            alias_index = lowered.index(alias.casefold())
            tail = line[alias_index + len(alias) :]
            numbers = [self._decimal(value) for value in NUMBER_PATTERN.findall(tail)]
            if not numbers:
                return None
            reference_low, reference_high = self._reference_values(tail, numbers)
            return IndicatorInput(
                code=code,
                name=standard_name,
                value=numbers[0],
                unit=self._unit(tail, standard_unit),
                referenceLow=reference_low,
                referenceHigh=reference_high,
            )
        return None

    def _matched_indicator(
        self, line: str, *, exact: bool = False
    ) -> tuple[str, str, str, str] | None:
        """Return the most specific alias so broad names cannot steal table rows."""
        lowered = line.casefold()
        matches = [
            (code, standard_name, standard_unit, alias)
            for code, standard_name, standard_unit, aliases in self.INDICATORS
            for alias in aliases
            if (alias.casefold() == lowered if exact else alias.casefold() in lowered)
        ]
        if not matches:
            return None
        return max(matches, key=lambda item: len(item[3]))

    def _parse_generic(self, line: str) -> IndicatorInput | None:
        match = re.match(
            r"^(?:\d+\s+)?(?P<name>[\u4e00-\u9fa5A-Za-z][\u4e00-\u9fa5A-Za-z0-9\- ]{1,30})\s+"
            r"(?P<value>[-+]?\d+(?:[.,]\d+)?)\s*(?P<unit>[%A-Za-zμµ/·^0-9]+)",
            line,
        )
        if match is None:
            return None
        name = match.group("name").strip()
        code = "unrecognized_" + hashlib.sha1(name.encode()).hexdigest()[:10]
        tail = line[match.end("value") :]
        numbers = [self._decimal(value) for value in NUMBER_PATTERN.findall(tail)]
        reference_low, reference_high = self._reference_values(tail, numbers, value_included=False)
        if reference_low is None or reference_high is None:
            return None
        return IndicatorInput(
            code=code,
            name=name,
            value=self._decimal(match.group("value")),
            unit=match.group("unit"),
            referenceLow=reference_low,
            referenceHigh=reference_high,
        )

    def _reference_values(
        self, tail: str, numbers: list[Decimal], *, value_included: bool = True
    ) -> tuple[Decimal | None, Decimal | None]:
        range_match = re.search(
            r"([-+]?\d+(?:[.,]\d+)?)\s*(?:-|~|—|至)\s*([-+]?\d+(?:[.,]\d+)?)",
            tail,
        )
        if range_match:
            return self._decimal(range_match.group(1)), self._decimal(range_match.group(2))
        candidates = numbers[1:] if value_included else numbers
        if len(candidates) >= 2:
            return candidates[-2], candidates[-1]
        return None, None

    def _unit(self, tail: str, fallback: str) -> str:
        units = re.findall(r"[%A-Za-zμµ]+(?:/[A-Za-z]+)?", tail)
        ignored = {"H", "L", "N", "HIGH", "LOW"}
        return next((unit for unit in reversed(units) if unit.upper() not in ignored), fallback)

    def _normalize(self, value: str) -> str:
        return re.sub(r"\s+", " ", value.replace("：", " ").replace(":", " ")).strip()

    def _clean_name(self, value: str) -> str:
        if re.search(
            r"[-+]?\d+(?:[.,]\d+)?\s*(?:-|~|—|至)\s*[-+]?\d+(?:[.,]\d+)?",
            value,
        ):
            return ""
        cleaned = ROW_NUMBER_PATTERN.sub("", value, count=1).strip()
        if self._matched_indicator(cleaned, exact=True) is not None:
            return cleaned
        if re.fullmatch(r"[%×xX^0-9A-Za-zμµ/·]+", cleaned):
            return ""
        return cleaned if re.search(r"[\u4e00-\u9fa5A-Za-z]", cleaned) else ""

    def _decimal(self, value: str) -> Decimal:
        return Decimal(value.replace(",", "."))


class MockOcrService(OcrService):
    def __init__(self) -> None:
        self.parser = IndicatorRowParser()

    def recognize(self, request: OcrRecognizeRequest) -> OcrRecognizeData:
        lines = [
            "空腹血糖 6.20 mmol/L 3.90-6.10",
            "糖化血红蛋白 5.80 % 4.00-6.00",
            "C反应蛋白 4.10 mg/L 0.00-3.00",
        ]
        return OcrRecognizeData(
            engine="STRUCTURED_DEMO_OCR_2.0",
            status="WAITING_CONFIRMATION",
            confidence=Decimal("0.9600"),
            indicators=self.parser.parse(lines),
            raw_lines=lines,
            warnings=["当前开发环境使用结构化演示识别结果，请人工核对后确认。"],
        )


class PaddleOcrService(OcrService):
    MAX_IMAGE_EDGE = 2400

    def __init__(self) -> None:
        self.parser = IndicatorRowParser()

    def recognize(self, request: OcrRecognizeRequest) -> OcrRecognizeData:
        if not request.download_url:
            raise ValueError("PaddleOCR requires a signed downloadUrl")
        path = self._download(request)
        prepared_path = path
        try:
            prepared_path = self._prepare_image(path, request.mime_type)
            lines, scores, boxes = self._recognize_file(prepared_path)
        finally:
            path.unlink(missing_ok=True)
            if prepared_path != path:
                prepared_path.unlink(missing_ok=True)
        layout_indicators = self.parser.parse_layout(lines, boxes)
        sequential_indicators = self.parser.parse(lines)
        merged: dict[str, IndicatorInput] = {}
        for item in [*layout_indicators, *sequential_indicators]:
            merged[item.code or item.name] = item
        indicators = list(merged.values())
        confidence = Decimal(str(round(sum(scores) / len(scores), 4))) if scores else Decimal("0")
        warnings: list[str] = []
        if not indicators:
            warnings.append("已识别报告文字，但未自动匹配出指标，请人工新增并校对。")
        if confidence < Decimal("0.80"):
            warnings.append("报告整体识别置信度较低，请重点核对数值与单位。")
        return OcrRecognizeData(
            engine="PaddleOCR-3.7.0/PP-OCRv6-small",
            status="WAITING_CONFIRMATION",
            confidence=confidence,
            indicators=indicators,
            raw_lines=lines,
            warnings=warnings,
        )

    def _download(self, request: OcrRecognizeRequest) -> Path:
        with httpx.Client(follow_redirects=True, timeout=60) as client:
            response = client.get(request.download_url or "")
            response.raise_for_status()
            content = response.content
        if not content:
            raise ValueError("OCR input file is empty")
        suffix = ".pdf" if request.mime_type == "application/pdf" else ".png"
        if request.mime_type == "image/jpeg":
            suffix = ".jpg"
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as target:
            target.write(content)
            return Path(target.name)

    def _prepare_image(self, path: Path, mime_type: str) -> Path:
        if not mime_type.startswith("image/"):
            return path
        with Image.open(path) as source:
            image = ImageOps.exif_transpose(source)
            image.thumbnail((self.MAX_IMAGE_EDGE, self.MAX_IMAGE_EDGE), Image.Resampling.LANCZOS)
            if image.mode not in {"RGB", "L"}:
                image = image.convert("RGB")
            with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as target:
                image.save(target, format="JPEG", quality=92, optimize=True)
                return Path(target.name)

    def _recognize_file(
        self, path: Path
    ) -> tuple[list[str], list[float], list[tuple[float, float, float, float]]]:
        lines: list[str] = []
        scores: list[float] = []
        boxes: list[tuple[float, float, float, float]] = []
        for result in self._pipeline().predict(str(path)):
            payload = getattr(result, "json", result)
            if callable(payload):
                payload = payload()
            if not isinstance(payload, dict):
                continue
            data = payload.get("res", payload)
            page_lines = [
                str(item).strip()
                for item in self._as_list(data.get("rec_texts"))
                if str(item).strip()
            ]
            page_scores = [float(item) for item in self._as_list(data.get("rec_scores"))]
            page_boxes = [self.parser._box(item) for item in self._as_list(data.get("rec_boxes"))]
            lines.extend(page_lines)
            scores.extend(page_scores[: len(page_lines)])
            boxes.extend(box for box in page_boxes[: len(page_lines)] if box is not None)
        return lines, scores, boxes

    def warm_up(self) -> None:
        self._pipeline()

    def _as_list(self, value: Any) -> list[Any]:
        if value is None:
            return []
        if hasattr(value, "tolist"):
            converted = value.tolist()
            return converted if isinstance(converted, list) else [converted]
        return list(value) if isinstance(value, list | tuple) else [value]

    @staticmethod
    @lru_cache(maxsize=1)
    def _pipeline() -> Any:
        from paddleocr import PaddleOCR  # type: ignore[import-untyped]

        return PaddleOCR(
            text_detection_model_name="PP-OCRv6_small_det",
            text_recognition_model_name="PP-OCRv6_small_rec",
            use_doc_orientation_classify=False,
            use_doc_unwarping=False,
            use_textline_orientation=False,
            device="cpu",
        )


def build_ocr_service() -> OcrService:
    mode = os.getenv("RAYK_OCR_MODE", "mock").strip().lower()
    return PaddleOcrService() if mode == "paddle" else MockOcrService()
