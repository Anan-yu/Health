import hashlib
import os
import re
import tempfile
from abc import ABC, abstractmethod
from collections.abc import Iterable
from dataclasses import dataclass
from decimal import Decimal
from difflib import SequenceMatcher
from functools import lru_cache
from pathlib import Path
from statistics import median
from typing import Any

import httpx
from PIL import Image, ImageEnhance, ImageFilter, ImageOps

from app.schemas.indicator import IndicatorInput
from app.schemas.ocr import OcrRecognizeData, OcrRecognizeRequest

NUMBER_PATTERN = re.compile(r"[-+]?\d+(?:[.,]\d+)?")
ROW_NUMBER_PATTERN = re.compile(r"^\s*\d{1,3}\s*")
NUMERIC_CELL_PATTERN = re.compile(
    r"^\s*(?:[↑↓HhLl*]+\s*)?([-+]?\d+(?:[.,]\d+)?)(?:\s*[↑↓HhLl*]+)?\s*$"
)
RANGE_PATTERN = re.compile(r"([-+]?\d+(?:[.,]\d+)?)\s*(?:-|~|—|–|至)\s*([-+]?\d+(?:[.,]\d+)?)")
HEADER_NAMES = {
    "项目",
    "检验项目",
    "检测项目",
    "检查项目",
    "测定项目",
    "项目名称",
}
HEADER_VALUES = {"结果", "检验结果", "检测结果", "测定结果", "数值"}
HEADER_UNITS = {"单位", "结果单位"}
HEADER_REFERENCES = {
    "参考范围",
    "参考区间",
    "参考值",
    "正常范围",
    "生物参考区间",
}
NON_INDICATOR_NAMES = {
    "姓名",
    "性别",
    "年龄",
    "出生日期",
    "报告日期",
    "送检日期",
    "采样日期",
    "科室",
    "病区",
    "床号",
    "标本",
    "标本类型",
    "临床诊断",
    "医生",
    "审核者",
    "检验者",
}
OCR_NAME_CORRECTIONS = {
    "呷离子": "钾离子",
    "内离子": "钠离子",
    "炭酸氢根": "碳酸氢根",
    "萄糖": "葡萄糖",
    "油三酯": "甘油三酯",
    "酯蛋白A1": "载脂蛋白A1",
    "清磷": "血清磷",
    "清镁": "血清镁",
    "清钙": "血清钙",
    "素氮": "尿素氮",
    "酐": "肌酐",
}
# Generous physiological and laboratory-reference bounds used only to catch column shifts and
# misplaced table cells. These are not diagnostic thresholds.
INDICATOR_VALUE_BOUNDS: dict[str, tuple[str, str]] = {
    "potassium": ("0.5", "15"),
    "sodium": ("50", "250"),
    "chloride": ("30", "200"),
    "calcium": ("0.2", "8"),
    "magnesium": ("0.1", "5"),
    "phosphorus": ("0.1", "8"),
    "bicarbonate": ("2", "80"),
    "fasting_glucose": ("0.5", "50"),
    "total_protein": ("10", "200"),
    "albumin": ("5", "100"),
    "globulin": ("1", "100"),
    "uric_acid": ("10", "2000"),
    "creatinine": ("5", "2000"),
    "triglyceride": ("0.01", "50"),
    "total_cholesterol": ("0.1", "30"),
    "hdl": ("0.01", "10"),
    "ldl": ("0.01", "20"),
    "apoa1": ("0.01", "10"),
    "apob": ("0.01", "10"),
    "prealbumin": ("1", "2000"),
}
INDICATOR_REFERENCE_HIGH_BOUNDS: dict[str, tuple[str, str]] = {
    "potassium": ("2", "15"),
    "sodium": ("100", "220"),
    "chloride": ("60", "180"),
    "calcium": ("1", "8"),
    "magnesium": ("0.2", "5"),
    "phosphorus": ("0.2", "6"),
    "bicarbonate": ("10", "60"),
    "fasting_glucose": ("2", "30"),
    "total_bilirubin": ("5", "100"),
    "direct_bilirubin": ("1", "80"),
    "indirect_bilirubin": ("3", "100"),
    "total_protein": ("30", "220"),
    "albumin": ("20", "120"),
    "globulin": ("10", "100"),
    "uric_acid": ("100", "1500"),
    "creatinine": ("20", "1000"),
    "urea": ("1", "60"),
    "triglyceride": ("0.1", "20"),
    "total_cholesterol": ("1", "20"),
    "hdl": ("0.1", "8"),
    "ldl": ("0.1", "15"),
    "apoa1": ("0.1", "5"),
    "apob": ("0.1", "5"),
    "prealbumin": ("20", "2000"),
    "lactate_dehydrogenase": ("40", "2000"),
}


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
        ("alkaline_phosphatase", "碱性磷酸酶", "U/L", ("碱性磷酸酶", "ALP", "AKP")),
        ("lactate_dehydrogenase", "乳酸脱氢酶", "U/L", ("乳酸脱氢酶", "LDH")),
        ("amylase", "淀粉酶", "U/L", ("淀粉酶", "血淀粉酶", "AMY")),
        ("total_bilirubin", "总胆红素", "μmol/L", ("总胆红素", "TBIL")),
        ("direct_bilirubin", "直接胆红素", "μmol/L", ("直接胆红素", "DBIL")),
        ("indirect_bilirubin", "间接胆红素", "μmol/L", ("间接胆红素", "IBIL")),
        ("total_protein", "总蛋白", "g/L", ("总蛋白", "血清总蛋白", "TP")),
        ("albumin", "白蛋白", "g/L", ("白蛋白", "ALB")),
        ("globulin", "球蛋白", "g/L", ("球蛋白", "GLOB", "GLB")),
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
        ("phosphorus", "磷", "mmol/L", ("血清磷", "无机磷", "磷")),
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

        header_tokens = [token for token in tokens if self._header_kind(token.text) is not None]
        name_headers = sorted(
            (token for token in header_tokens if self._header_kind(token.text) == "name"),
            key=lambda token: token.left,
        )
        first_row_y = (
            max(token.center_y for token in header_tokens) + 8
            if len(header_tokens) >= 2
            else min(token.center_y for token in tokens)
        )
        table_tokens = [token for token in tokens if token.center_y > first_row_y]
        if not table_tokens:
            return []

        columns = self._layout_columns(table_tokens, name_headers)
        parsed: dict[str, IndicatorInput] = {}
        header_columns = self._layout_columns(header_tokens, name_headers)
        for column_index, column in enumerate(columns):
            for row in self._group_rows(column):
                item = self._parse_layout_row(row)
                if item is not None:
                    parsed[item.code or item.name] = item
            if column_index < len(header_columns):
                for item in self._parse_header_anchored_rows(column, header_columns[column_index]):
                    parsed[item.code or item.name] = item
        return list(parsed.values())

    def _parse_layout_row(self, row: list["LayoutToken"]) -> IndicatorInput | None:
        ordered = sorted(row, key=lambda token: token.left)
        if len(ordered) < 2:
            return None
        known_row = self._parse_known(" ".join(token.text for token in ordered))
        if known_row is not None:
            return known_row
        name_index = next(
            (
                index
                for index, token in enumerate(ordered)
                if self._is_indicator_name(self._clean_name(token.text))
            ),
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
                if self._numeric_cell(token.text) is not None
            ),
            None,
        )
        if value_index is None:
            return None
        value_token = self._numeric_cell(ordered[value_index].text)
        reference_text = " ".join(token.text for token in ordered[value_index + 1 :])
        reference_low, reference_high = self._reference_values(
            reference_text,
            [self._decimal(value) for value in NUMBER_PATTERN.findall(reference_text)],
            value_included=False,
        )
        if value_token is None:
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
        if not name_token or not unit or reference_low is None or reference_high is None:
            return None
        return IndicatorInput(
            code="unrecognized_" + hashlib.sha1(name_token.encode()).hexdigest()[:10],
            name=name_token,
            value=self._decimal(value_token),
            unit=unit,
            referenceLow=reference_low,
            referenceHigh=reference_high,
        )

    def _parse_header_anchored_rows(
        self, tokens: list["LayoutToken"], headers: list["LayoutToken"]
    ) -> list[IndicatorInput]:
        """Use header x-positions and analyte names as row anchors for skewed report photos."""
        by_kind: dict[str, LayoutToken] = {}
        for header in sorted(headers, key=lambda token: token.left):
            kind = self._header_kind(header.text)
            if kind is not None and kind not in by_kind:
                by_kind[kind] = header
        if "name" not in by_kind or "value" not in by_kind:
            return []

        ordered_headers = sorted(by_kind.items(), key=lambda item: item[1].center_x)
        cell_boundaries = [
            (left[1].center_x + right[1].center_x) / 2
            for left, right in zip(ordered_headers, ordered_headers[1:], strict=False)
        ]
        cell_kinds = [kind for kind, _ in ordered_headers]

        def token_kind(token: LayoutToken) -> str:
            index = sum(token.center_x >= boundary for boundary in cell_boundaries)
            return cell_kinds[min(index, len(cell_kinds) - 1)]

        name_tokens = [
            token
            for token in tokens
            if token_kind(token) == "name" and self._is_indicator_name(self._clean_name(token.text))
        ]
        if not name_tokens:
            return []
        name_tokens.sort(key=lambda token: token.center_y)
        row_boundaries = [
            (upper.center_y + lower.center_y) / 2
            for upper, lower in zip(name_tokens, name_tokens[1:], strict=False)
        ]
        parsed: list[IndicatorInput] = []
        for index, _name_token in enumerate(name_tokens):
            row_top = row_boundaries[index - 1] if index else float("-inf")
            row_bottom = row_boundaries[index] if index < len(row_boundaries) else float("inf")
            row = [token for token in tokens if row_top <= token.center_y < row_bottom]
            item = self._parse_layout_row(row)
            if item is not None:
                parsed.append(item)
        return parsed

    def _layout_columns(
        self, tokens: list["LayoutToken"], name_headers: list["LayoutToken"]
    ) -> list[list["LayoutToken"]]:
        """Split repeated side-by-side laboratory tables without assuming a fixed page midpoint."""
        if len(name_headers) <= 1:
            return [tokens]
        # A repeated name header marks the start of the next table. Splitting at the midpoint
        # between name headers cuts the result/unit/reference columns off the left table.
        boundaries = [header.left - 4 for header in name_headers[1:]]
        columns: list[list[LayoutToken]] = [[] for _ in name_headers]
        for token in tokens:
            column_index = sum(token.center_x >= boundary for boundary in boundaries)
            columns[column_index].append(token)
        return [column for column in columns if column]

    def _header_kind(self, value: str) -> str | None:
        normalized = re.sub(r"\s+", "", value)
        if normalized in HEADER_NAMES:
            return "name"
        if normalized in HEADER_VALUES:
            return "value"
        if normalized in HEADER_UNITS:
            return "unit"
        if normalized in HEADER_REFERENCES:
            return "reference"
        return None

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
        if not self._is_indicator_name(name):
            return None
        value_text = lines[index + 1].strip()
        numeric_value = self._numeric_cell(value_text)
        if numeric_value is None:
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
            (cell for cell in following if RANGE_PATTERN.search(cell)),
            "",
        )
        range_numbers = (
            [self._decimal(value) for value in NUMBER_PATTERN.findall(range_text)]
            if range_text
            else []
        )
        reference_low, reference_high = self._reference_values(
            range_text, range_numbers, value_included=False
        )

        matched = self._matched_indicator(name)
        combined_tail = " ".join(following)
        if matched is not None:
            code, standard_name, standard_unit, _ = matched
            return IndicatorInput(
                code=code,
                name=standard_name,
                value=self._decimal(numeric_value),
                unit=self._unit(combined_tail, standard_unit),
                referenceLow=reference_low,
                referenceHigh=reference_high,
            )
        if reference_low is None or reference_high is None:
            return None
        if re.search(r"[\u4e00-\u9fa5A-Za-z]", name) is None:
            return None
        return IndicatorInput(
            code="unrecognized_" + hashlib.sha1(name.encode()).hexdigest()[:10],
            name=name,
            value=self._decimal(numeric_value),
            unit=self._unit(combined_tail, "index"),
            referenceLow=reference_low,
            referenceHigh=reference_high,
        )

    def _parse_known(self, line: str) -> IndicatorInput | None:
        corrected = self._correct_common_ocr_errors(line)
        lowered = corrected.casefold()
        matched = self._matched_indicator(line)
        if matched is not None:
            code, standard_name, standard_unit, alias = matched
            alias_index = lowered.find(alias.casefold())
            if alias_index >= 0:
                tail = corrected[alias_index + len(alias) :]
            else:
                first_number = NUMBER_PATTERN.search(corrected)
                tail = corrected[first_number.start() :] if first_number is not None else ""
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
        corrected = self._correct_common_ocr_errors(line)
        lowered = corrected.casefold()
        matches = [
            (code, standard_name, standard_unit, alias)
            for code, standard_name, standard_unit, aliases in self.INDICATORS
            for alias in aliases
            if (alias.casefold() == lowered if exact else alias.casefold() in lowered)
        ]
        if matches:
            return max(matches, key=lambda item: len(item[3]))
        if exact:
            return self._fuzzy_indicator(corrected)
        name_candidate = re.split(NUMBER_PATTERN, corrected, maxsplit=1)[0]
        name_candidate = ROW_NUMBER_PATTERN.sub("", name_candidate, count=1).strip()
        return self._fuzzy_indicator(name_candidate)

    def _fuzzy_indicator(self, value: str) -> tuple[str, str, str, str] | None:
        candidate = re.sub(r"[\s\-—_()（）]+", "", value).casefold()
        if len(candidate) < 3:
            return None
        ranked: list[tuple[float, tuple[str, str, str, str]]] = []
        for code, standard_name, standard_unit, aliases in self.INDICATORS:
            for alias in aliases:
                normalized_alias = re.sub(r"[\s\-—_()（）]+", "", alias).casefold()
                if len(normalized_alias) < 3:
                    continue
                score = SequenceMatcher(None, candidate, normalized_alias).ratio()
                if (
                    min(len(candidate), len(normalized_alias)) >= 3
                    and (candidate in normalized_alias or normalized_alias in candidate)
                    and abs(len(candidate) - len(normalized_alias)) <= 2
                ):
                    score = max(score, 0.9)
                ranked.append((score, (code, standard_name, standard_unit, alias)))
        if not ranked:
            return None
        ranked.sort(key=lambda item: item[0], reverse=True)
        best_score, best = ranked[0]
        competing_scores = [score for score, item in ranked[1:] if item[0] != best[0]]
        second_score = max(competing_scores, default=0.0)
        if best_score < 0.78 or best_score - second_score < 0.06:
            return None
        return best

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
        range_match = RANGE_PATTERN.search(tail)
        if range_match:
            return self._decimal(range_match.group(1)), self._decimal(range_match.group(2))
        upper_match = re.search(r"(?:≤|<|不超过)\s*([-+]?\d+(?:[.,]\d+)?)", tail)
        if upper_match:
            return None, self._decimal(upper_match.group(1))
        lower_match = re.search(r"(?:≥|>|不少于)\s*([-+]?\d+(?:[.,]\d+)?)", tail)
        if lower_match:
            return self._decimal(lower_match.group(1)), None
        candidates = numbers[1:] if value_included else numbers
        if len(candidates) >= 2:
            return candidates[-2], candidates[-1]
        return None, None

    def _unit(self, tail: str, fallback: str) -> str:
        normalized = tail.replace("μ", "µ").replace("×", "x").replace("²", "2").replace("³", "3")
        units = re.findall(
            r"(?:x?10\^\d{1,2}/[A-Za-z]+|[%A-Za-zµ]+(?:/[A-Za-z]+)?(?:/1\.73m2)?)",
            normalized,
        )
        ignored = {"H", "L", "N", "HIGH", "LOW"}
        candidate = next(
            (unit for unit in reversed(units) if unit.upper() not in ignored),
            fallback,
        )
        if fallback and fallback not in {"index", "ratio"}:
            expected = self._normalize_unit(fallback)
            observed = self._normalize_unit(candidate)
            return candidate if observed == expected else fallback
        return fallback if fallback in {"index", "ratio"} else candidate

    def _normalize_unit(self, value: str) -> str:
        return (
            value.strip()
            .replace("μ", "µ")
            .replace("×", "x")
            .replace("²", "2")
            .replace("³", "3")
            .casefold()
        )

    def _normalize(self, value: str) -> str:
        return re.sub(r"\s+", " ", value.replace("：", " ").replace(":", " ")).strip()

    def _clean_name(self, value: str) -> str:
        if RANGE_PATTERN.search(value):
            return ""
        cleaned = ROW_NUMBER_PATTERN.sub("", value, count=1).strip()
        cleaned = self._correct_common_ocr_errors(cleaned)
        if self._matched_indicator(cleaned, exact=True) is not None:
            return cleaned
        if re.fullmatch(r"[%×xX^0-9A-Za-zμµ/·]+", cleaned):
            return ""
        return cleaned if re.search(r"[\u4e00-\u9fa5A-Za-z]", cleaned) else ""

    def _is_indicator_name(self, value: str) -> bool:
        compact = re.sub(r"[\s:：]+", "", value)
        if not compact or compact in NON_INDICATOR_NAMES:
            return False
        if self._header_kind(compact) is not None:
            return False
        return not any(
            compact.startswith(prefix)
            for prefix in ("姓名", "性别", "年龄", "日期", "科室", "标本", "诊断")
        )

    def _numeric_cell(self, value: str) -> str | None:
        match = NUMERIC_CELL_PATTERN.fullmatch(value)
        return match.group(1) if match is not None else None

    def _correct_common_ocr_errors(self, value: str) -> str:
        corrected = value
        for source, target in OCR_NAME_CORRECTIONS.items():
            if target not in corrected:
                corrected = corrected.replace(source, target)
        return corrected

    def _decimal(self, value: str) -> Decimal:
        return Decimal(value.replace(",", "."))


@dataclass(frozen=True)
class OcrQualityResult:
    indicators: list[IndicatorInput]
    confidence: Decimal
    status: str
    warnings: list[str]


class OcrQualityValidator:
    """Prevent structurally unsafe OCR rows from entering health assessment."""

    MIN_TEXT_CONFIDENCE = Decimal("0.55")
    MIN_COMBINED_CONFIDENCE = Decimal("0.68")
    MAX_REJECTED_RATIO = Decimal("0.20")
    MAX_UNKNOWN_RATIO = Decimal("0.35")

    def validate(
        self,
        indicators: list[IndicatorInput],
        text_confidence: Decimal,
        raw_lines: list[str],
    ) -> OcrQualityResult:
        accepted: list[IndicatorInput] = []
        warnings: list[str] = []
        rejected_count = 0
        grouped: dict[str, list[IndicatorInput]] = {}
        for item in indicators:
            grouped.setdefault(self._candidate_key(item), []).append(item)

        for candidates in grouped.values():
            valid_candidates = [item for item in candidates if self._valid_indicator(item)]
            if not valid_candidates:
                rejected_count += 1
                continue
            accepted.append(max(valid_candidates, key=self._candidate_score))

        if rejected_count:
            warnings.append(f"已拦截 {rejected_count} 项结构或数值异常的识别结果。")
        unknown_count = sum(1 for item in accepted if (item.code or "").startswith("unrecognized_"))
        if unknown_count:
            warnings.append(f"有 {unknown_count} 项为非常见指标，已保留原始名称。")

        total_groups = len(grouped)
        rejected_ratio = (
            Decimal(rejected_count) / Decimal(total_groups) if total_groups else Decimal("1")
        )
        unknown_ratio = (
            Decimal(unknown_count) / Decimal(len(accepted)) if accepted else Decimal("1")
        )
        structure_confidence = self._structure_confidence(accepted, total_groups)
        combined = Decimal(
            str(
                round(
                    float(text_confidence) * 0.65 + float(structure_confidence) * 0.35,
                    4,
                )
            )
        )
        retry_required = (
            not raw_lines
            or not accepted
            or text_confidence < self.MIN_TEXT_CONFIDENCE
            or combined < self.MIN_COMBINED_CONFIDENCE
            or (rejected_count > 0 and rejected_ratio >= self.MAX_REJECTED_RATIO)
            or unknown_ratio > self.MAX_UNKNOWN_RATIO
        )
        if retry_required:
            warnings.append("本次报告识别可靠性不足，请使用清晰、完整、正向拍摄的报告重新上传。")
        elif combined < Decimal("0.82"):
            warnings.append("部分内容版式较复杂，系统已完成结构校验并过滤低可信数据。")
        return OcrQualityResult(
            indicators=accepted,
            confidence=combined,
            status="RETRY_REQUIRED" if retry_required else "SUCCESS",
            warnings=warnings,
        )

    def _valid_indicator(self, item: IndicatorInput) -> bool:
        if not item.name.strip() or item.name.strip() in NON_INDICATOR_NAMES:
            return False
        if (item.code or "").startswith("unrecognized_") and len(item.name.strip()) < 2:
            return False
        if not item.value.is_finite() or abs(item.value) > Decimal("1000000000"):
            return False
        if (
            item.reference_low is not None
            and item.reference_high is not None
            and item.reference_low > item.reference_high
        ):
            return False
        if item.reference_low is not None and not item.reference_low.is_finite():
            return False
        if item.reference_high is not None and not item.reference_high.is_finite():
            return False
        if (
            item.reference_low is not None
            and item.reference_low > 0
            and item.value >= 0
            and item.value * Decimal("10") < item.reference_low
        ):
            return False
        if (
            item.reference_high is not None
            and item.reference_high > 0
            and item.value > item.reference_high * Decimal("10")
        ):
            return False
        return bool(item.unit.strip()) and self._medically_plausible(item)

    def _medically_plausible(self, item: IndicatorInput) -> bool:
        """Detect table-column shifts with broad data-quality bounds, not diagnostic cutoffs."""
        code = item.code or ""
        value_bounds = INDICATOR_VALUE_BOUNDS.get(code)
        if value_bounds is not None:
            minimum, maximum = (Decimal(bound) for bound in value_bounds)
            if item.value < minimum or item.value > maximum:
                return False

        reference_bounds = INDICATOR_REFERENCE_HIGH_BOUNDS.get(code)
        if reference_bounds is not None and item.reference_high is not None:
            minimum, maximum = (Decimal(bound) for bound in reference_bounds)
            if item.reference_high < minimum or item.reference_high > maximum:
                return False
        return True

    def _candidate_key(self, item: IndicatorInput) -> str:
        return item.code or item.name.strip()

    def _candidate_score(self, item: IndicatorInput) -> tuple[float, float]:
        score = 0.0
        if not (item.code or "").startswith("unrecognized_"):
            score += 0.25
        if item.reference_low is not None:
            score += 0.15
        if item.reference_high is not None:
            score += 0.15
        if (
            item.reference_low is not None
            and item.reference_high is not None
            and item.reference_low <= item.value <= item.reference_high
        ):
            score += 0.30
        if self._medically_plausible(item):
            score += 0.15
        # Prefer narrower, clinically realistic intervals when two parsers found the same row.
        interval_width = (
            float(item.reference_high - item.reference_low)
            if item.reference_low is not None and item.reference_high is not None
            else float("inf")
        )
        return score, -interval_width

    def _structure_confidence(self, accepted: list[IndicatorInput], total_groups: int) -> Decimal:
        if not total_groups or not accepted:
            return Decimal("0")
        row_scores: list[float] = []
        for item in accepted:
            score = 0.72
            if item.reference_low is not None or item.reference_high is not None:
                score += 0.16
            if item.reference_low is not None and item.reference_high is not None:
                score += 0.06
            if not (item.code or "").startswith("unrecognized_"):
                score += 0.06
            row_scores.append(min(score, 1.0))
        retention = len(accepted) / total_groups
        return Decimal(str(round((sum(row_scores) / len(row_scores)) * retention, 4)))


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
    MIN_IMAGE_EDGE = 2000
    MAX_IMAGE_EDGE = 4200

    def __init__(self) -> None:
        self.parser = IndicatorRowParser()
        self.validator = OcrQualityValidator()

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
        text_confidence = (
            Decimal(str(round(sum(scores) / len(scores), 4))) if scores else Decimal("0")
        )
        # Keep candidates from both parsers. The validator selects the safest row for each
        # indicator after checking units, reference intervals and broad laboratory plausibility.
        quality = self.validator.validate(
            [*sequential_indicators, *layout_indicators], text_confidence, lines
        )
        warnings: list[str] = []
        if not quality.indicators:
            warnings.append("已识别报告文字，但没有提取到可安全用于评估的检验指标。")
        if text_confidence < Decimal("0.80"):
            warnings.append("报告文字清晰度偏低，已启用低可信数据保护。")
        warnings.extend(quality.warnings)
        return OcrRecognizeData(
            engine="PaddleOCR-3.7.0/PP-OCRv6-small-enhanced",
            status=quality.status,
            confidence=quality.confidence,
            indicators=quality.indicators,
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
            longest_edge = max(image.size)
            if longest_edge < self.MIN_IMAGE_EDGE:
                scale = self.MIN_IMAGE_EDGE / max(longest_edge, 1)
                image = image.resize(
                    (
                        max(1, round(image.width * scale)),
                        max(1, round(image.height * scale)),
                    ),
                    Image.Resampling.LANCZOS,
                )
            elif longest_edge > self.MAX_IMAGE_EDGE:
                image.thumbnail(
                    (self.MAX_IMAGE_EDGE, self.MAX_IMAGE_EDGE), Image.Resampling.LANCZOS
                )
            image = ImageOps.grayscale(image)
            image = ImageOps.autocontrast(image, cutoff=1)
            image = ImageEnhance.Contrast(image).enhance(1.12)
            image = image.filter(ImageFilter.UnsharpMask(radius=1.2, percent=125, threshold=3))
            with tempfile.NamedTemporaryFile(delete=False, suffix=".png") as target:
                image.save(target, format="PNG", optimize=True)
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
            use_doc_orientation_classify=True,
            use_doc_unwarping=True,
            use_textline_orientation=True,
            device="cpu",
        )


def build_ocr_service() -> OcrService:
    mode = os.getenv("RAYK_OCR_MODE", "mock").strip().lower()
    return PaddleOcrService() if mode == "paddle" else MockOcrService()
