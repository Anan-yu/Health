import hashlib
import os
import re
import tempfile
from abc import ABC, abstractmethod
from collections.abc import Iterable, Sequence
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
from difflib import SequenceMatcher
from functools import lru_cache
from pathlib import Path
from statistics import median
from typing import Any

import httpx
from PIL import Image, ImageEnhance, ImageFilter, ImageOps

from app.ocr.qwen import QwenOcrClient, QwenOcrError, extract_qwen_document
from app.schemas.indicator import IndicatorInput
from app.schemas.ocr import OcrFinding, OcrRecognizeData, OcrRecognizeRequest

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

# Header, identity and basic-profile fields are useful document context, but they are not
# laboratory indicators.  Keep this guard shared by every OCR source because supplemental
# PDF/Paddle rows are merged after the cloud result and must not re-introduce metadata.
NON_INDICATOR_CONTAINS = tuple(
    value.casefold()
    for value in (
        "\u59d3\u540d",  # name
        "\u6027\u522b",  # sex
        "\u5e74\u9f84",  # age
        "\u51fa\u751f\u65e5\u671f",
        "\u4f53\u68c0\u65e5\u671f",
        "\u68c0\u67e5\u65e5\u671f",
        "\u62a5\u544a\u65e5\u671f",
        "\u6253\u5370\u65e5\u671f",
        "\u9001\u68c0\u65e5\u671f",
        "\u91c7\u6837\u65e5\u671f",
        "\u62a5\u544a\u65f6\u95f4",
        "\u54a8\u8be2\u7535\u8bdd",
        "\u8054\u7cfb\u7535\u8bdd",
        "\u624b\u673a\u53f7\u7801",
        "\u624b\u673a\u53f7",
        "\u8eab\u4efd\u8bc1",
        "\u62a5\u544a\u7f16\u53f7",
        "\u62a5\u544a\u53f7",
        "\u4f53\u68c0\u7f16\u53f7",
        "\u4f4f\u9662\u53f7",
        "\u95e8\u8bca\u53f7",
        "床位号",
        "床号",
        "检查号",
        "检验号",
        "样本号",
        "申请单号",
        "仪器型号",
        "设备编号",
        "\u6761\u7801\u53f7",
    )
)
NON_INDICATOR_PREFIXES = tuple(
    value.casefold()
    for value in (
        "\u8eab\u9ad8",
        "\u4f53\u91cd",
        "\u8170\u56f4",
        "\u81c0\u56f4",
        "\u4f53\u8102\u7387",
        "BMI",
        "\u4f53\u8d28\u6307\u6570",
        "\u8840\u538b",
        "\u8109\u640f",
        "\u4f53\u6e29",
        "\u533b\u9662",
        "\u79d1\u5ba4",
        "\u75c5\u533a",
        "\u5e8a\u53f7",
        "\u6807\u672c\u7c7b\u578b",
        "\u4e34\u5e8a\u8bca\u65ad",
    )
)


def is_non_indicator_name(name: str) -> bool:
    compact = re.sub(r"[\s:：|]+", "", (name or "")).casefold()
    if not compact or compact in {value.casefold() for value in NON_INDICATOR_NAMES}:
        return True
    return any(marker in compact for marker in NON_INDICATOR_CONTAINS) or any(
        compact.startswith(prefix) for prefix in NON_INDICATOR_PREFIXES
    )


NON_FINDING_METADATA_CONTAINS = tuple(
    value.casefold()
    for value in (
        "姓名",
        "性别",
        "年龄",
        "出生日期",
        "体检日期",
        "检查日期",
        "报告日期",
        "打印日期",
        "送检日期",
        "采样日期",
        "报告时间",
        "咨询电话",
        "联系电话",
        "手机号",
        "身份证",
        "报告编号",
        "报告号",
        "体检编号",
        "住院号",
        "门诊号",
        "床位号",
        "床号",
        "病区",
        "科室",
        "条码号",
        "检查号",
        "检验号",
        "样本号",
        "申请单号",
        "仪器型号",
        "设备编号",
    )
)


def is_non_finding_metadata_name(name: str) -> bool:
    compact = re.sub(r"[\s:：|/]+", "", (name or "")).casefold()
    return not compact or any(marker in compact for marker in NON_FINDING_METADATA_CONTAINS)
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

    def parse_pdf_tables(self, path: Path) -> list[IndicatorInput]:
        """Read laboratory rows directly from an electronic PDF table.

        Native PDF tables are materially safer than flattening the whole document into lines:
        the analyte, result and reference range remain in their original columns, so patient
        names, phone numbers and dates cannot drift into an indicator row. Scanned PDFs simply
        return no rows and continue through the visual OCR fallback.
        """
        try:
            import pdfplumber
        except ImportError:
            return []

        parsed: dict[str, IndicatorInput] = {}
        try:
            with pdfplumber.open(path) as document:
                for page in document.pages:
                    for table in page.extract_tables():
                        rows = self._merge_pdf_table_continuations(table)
                        for row in rows:
                            item = self._parse_pdf_table_row(row)
                            if item is not None:
                                parsed[item.code or item.name] = item
        except Exception:
            # Password-protected, malformed and image-only PDFs must still use visual OCR.
            return []
        return list(parsed.values())

    def parse_pdf_findings(self, path: Path) -> list[OcrFinding]:
        """Keep source-ordered examination rows from an electronic physical-exam PDF.

        Each category name, item name and complete result is retained as printed. Numeric values
        may also be parsed into ``indicators`` for assessment, but remain here so the source view
        never loses their original category, unit, reference range or relative position.
        """
        try:
            import pdfplumber
        except ImportError:
            return []

        try:
            with pdfplumber.open(path) as document:
                # A native text layer follows the PDF's visual reading order. It is therefore
                # the authoritative source for electronic physical-exam reports: starting with
                # tables and appending native text later duplicated whole sections and moved
                # early items (for example 肝纤维检测) behind later categories.
                findings: list[OcrFinding] = []
                seen: set[tuple[str, str, str]] = set()
                self._parse_native_pdf_findings(document.pages, findings, seen)
                if findings:
                    return findings

                # Some electronic PDFs expose ruled tables but no usable page text. Retain the
                # table parser strictly as a fallback instead of mixing two extraction passes.
                return self._parse_pdf_table_findings(document.pages)
        except Exception:
            return []

    def _parse_pdf_table_findings(self, pages: Sequence[object]) -> list[OcrFinding]:
        findings: list[OcrFinding] = []
        seen: set[tuple[str, str, str]] = set()
        for page in pages:
            section = "体检结果"
            last_finding_index: int | None = None
            summary_mode = False
            extract_tables = getattr(page, "extract_tables", None)
            if not callable(extract_tables):
                continue
            for table in extract_tables():
                for raw_row in table:
                    row = [re.sub(r"\s+", " ", str(cell or "")).strip() for cell in raw_row]
                    if not row:
                        continue
                    first = row[0] if len(row) > 0 else ""
                    second = row[1] if len(row) > 1 else ""
                    section_match = re.fullmatch(r"【\s*(.+?)\s*】", first)
                    if section_match:
                        section = section_match.group(1).strip()
                        last_finding_index = None
                        summary_mode = False
                        continue
                    if first in {"小结：", "小结", "检查小结：", "检查小结"}:
                        summary_mode = True
                        last_finding_index = None
                        if second:
                            last_finding_index = self._append_pdf_finding(
                                findings, seen, section, "检查小结", second
                            )
                        continue
                    if not first and second and last_finding_index is not None:
                        previous = findings[last_finding_index]
                        combined = f"{previous.result}{second}".strip()
                        seen.discard((previous.section, previous.item, previous.result))
                        findings[last_finding_index] = OcrFinding(
                            section=previous.section,
                            item=previous.item,
                            result=combined,
                        )
                        seen.add((previous.section, previous.item, combined))
                        continue
                    if summary_mode and first and not second:
                        last_finding_index = self._append_pdf_finding(
                            findings, seen, section, "检查小结", first
                        )
                        continue
                    summary_mode = False
                    if not self._is_pdf_finding_row(first, second):
                        last_finding_index = None
                        continue
                    if self._parse_pdf_table_row(row) is not None:
                        last_finding_index = None
                        continue
                    last_finding_index = self._append_pdf_finding(
                        findings, seen, section, self._clean_pdf_name(first), second
                    )
        return findings

    def _parse_native_pdf_findings(
        self,
        pages: Sequence[object],
        findings: list[OcrFinding],
        seen: set[tuple[str, str, str]],
    ) -> None:
        """Extract original results from PDFs whose text has no ruled table objects.

        Many Chinese physical-exam systems draw columns as positioned glyphs rather than PDF
        tables. ``pdfplumber.extract_tables`` therefore returns nothing although the document
        contains a lossless text layer. This parser follows the visible section/result labels and
        deliberately ignores report metadata and explanatory boilerplate.
        """
        section = "体检结果"
        skip_explanation = False
        clinical_started = False
        for page in pages:
            extract_text = getattr(page, "extract_text", None)
            if not callable(extract_text):
                continue
            text = extract_text(x_tolerance=2, y_tolerance=3) or ""
            lines = [re.sub(r"\s+", " ", line).strip() for line in text.splitlines()]
            lines = [line for line in lines if line]
            if not lines:
                continue

            if any(
                marker in line
                for line in lines
                for marker in ("体检明细", "超声检查报告单")
            ):
                clinical_started = True
            if not clinical_started:
                continue

            # The explanatory chapter repeats findings together with generic medical education;
            # the original results are captured from the summary and detail chapters instead.
            if any("主要异常结果解读" in line for line in lines):
                skip_explanation = True
            if any("体检明细" in line for line in lines):
                skip_explanation = False
            if skip_explanation:
                continue

            index = 0
            while index < len(lines):
                line = lines[index]
                section_match = re.match(r"^[★]([^：:]+)[：:]?$", line)
                detail_section_match = re.match(r"^【\s*(.+?)\s*】(?:\s*检查结果.*)?$", line)
                matched_section = section_match or detail_section_match
                if matched_section:
                    section = matched_section.group(1).strip()
                    index += 1
                    continue

                if line.startswith("检查部位 "):
                    section = line.removeprefix("检查部位 ").strip() or section
                    index += 1
                    continue

                if line.startswith("检查所见 ") or line.startswith("诊断意见 "):
                    label = "检查所见" if line.startswith("检查所见 ") else "诊断意见"
                    prefix = f"{label} "
                    parts = [line.removeprefix(prefix).strip()]
                    index += 1
                    while index < len(lines) and not self._is_native_pdf_boundary(lines[index]):
                        parts.append(lines[index])
                        index += 1
                    result = "".join(part for part in parts if part)
                    self._append_pdf_finding(findings, seen, section, label, result)
                    continue

                if line in {"小结：", "小结", "检查小结：", "检查小结"}:
                    index += 1
                    summary_parts: list[str] = []
                    while index < len(lines) and not self._is_native_pdf_boundary(lines[index]):
                        summary_parts.append(lines[index])
                        index += 1
                    for result in summary_parts:
                        if not self._is_pdf_metadata_line(result):
                            self._append_pdf_finding(
                                findings, seen, section, "检查小结", result
                            )
                    continue

                row = self._split_native_pdf_result(line)
                if row is not None:
                    item, result = row
                    if self._is_pdf_finding_row(item, result):
                        # Preserve every source row in its original category. Numeric rows are
                        # also parsed separately as indicators for assessment, but omitting them
                        # here would make the source-category view incomplete or move them away
                        # from the section in which the hospital report placed them.
                        self._append_pdf_finding(findings, seen, section, item, result)
                index += 1

    @classmethod
    def _is_native_pdf_boundary(cls, line: str) -> bool:
        return bool(
            re.match(
                r"^(?:【|★|检查部位 |检查所见 |诊断意见 |报告时间|打印日期|注[:：]|第 \d+)",
                line,
            )
            or line in {"小结：", "小结", "检查小结：", "检查小结"}
        )

    @staticmethod
    def _is_pdf_metadata_line(line: str) -> bool:
        compact = re.sub(r"\s+", "", line)
        return bool(re.fullmatch(r"第\d+[⻚页]", compact)) or any(
            marker in compact
            for marker in (
                "报告时间",
                "打印日期",
                "检查者",
                "审核者",
                "检查医生",
                "审核医生",
                "本报告仅供",
                "体检机构",
                "医院地址",
                "联系电话",
                "报告说明",
                "初审医师",
                "总检医师",
                "尊敬的",
                "健康体检的目的",
                "第1页",
                "第2页",
                "第3页",
                "第4页",
                "第5页",
            )
        )

    @classmethod
    def _split_native_pdf_result(cls, line: str) -> tuple[str, str] | None:
        if cls._is_pdf_metadata_line(line):
            return None
        compact = line.strip()
        if not compact or compact.startswith(
            ("▍", "_", "收费项目", "是否弃检", "注：", "注:")
        ):
            return None
        colon = re.match(r"^([^：:]{1,30})[：:]\s*(.+)$", compact)
        if colon:
            return colon.group(1).strip(), colon.group(2).strip()
        spaced = re.match(r"^(.{1,24}?)\s+(.+)$", compact)
        if spaced is None:
            return None
        item, result = spaced.group(1).strip(), spaced.group(2).strip()
        if item in {"检查科室", "科室名称", "收费项目", "收费项目名称"}:
            return None
        if result in {"检查结果", "提示", "参考值"}:
            return None
        return item, result

    @staticmethod
    def _append_pdf_finding(
        findings: list[OcrFinding],
        seen: set[tuple[str, str, str]],
        section: str,
        item: str,
        result: str,
    ) -> int | None:
        cleaned_item = re.sub(r"\s+", " ", item).strip(" ：:")
        cleaned_result = re.sub(r"\s+", " ", result).strip()
        cleaned_result = re.split(
            r"(?:报告时间|打印日期|检查者[:：]|审核者[:：]|检查医生|审核医生)",
            cleaned_result,
            maxsplit=1,
        )[0].strip()
        if (
            not cleaned_item
            or not cleaned_result
            or is_non_finding_metadata_name(cleaned_item)
        ):
            return None
        key = (section or "体检结果", cleaned_item, cleaned_result)
        if key in seen:
            return None
        seen.add(key)
        findings.append(OcrFinding(section=key[0], item=key[1], result=key[2]))
        return len(findings) - 1

    @staticmethod
    def _is_pdf_finding_row(name: str, result: str) -> bool:
        if not name or not result:
            return False
        compact_name = re.sub(r"\s+", "", name)
        excluded = (
            "检查科室",
            "收费项目",
            "检查结果",
            "参考值",
            "提示",
            "报告时间",
            "检查者",
            "审核者",
            "医生签字",
            "姓名",
            "性别",
            "年龄",
            "身份证",
            "咨询电话",
            "打印日期",
            "体检日期",
        )
        if any(marker in compact_name for marker in excluded):
            return False
        if compact_name in {"是否弃检", "弃检原因", "一般检查", "外科检查", "内科检查"}:
            return False
        return bool(re.search(r"[\u4e00-\u9fa5A-Za-z]", compact_name))

    @staticmethod
    def _merge_pdf_table_continuations(table: list[list[str | None]]) -> list[list[str]]:
        rows = [[re.sub(r"\s+", " ", str(cell or "")).strip() for cell in row] for row in table]
        merged: list[list[str]] = []
        code_only = re.compile(r"^[（(][A-Za-z][A-Za-z0-9%#\-/]*[）)]$")
        for row in rows:
            if (
                merged
                and row
                and code_only.fullmatch(row[0])
                and not any(cell for cell in row[1:])
                and merged[-1][1]
            ):
                merged[-1][0] = f"{merged[-1][0]}{row[0]}"
                continue
            merged.append(row)
        return merged

    def _parse_pdf_table_row(self, row: list[str]) -> IndicatorInput | None:
        if len(row) < 2:
            return None
        name = self._clean_pdf_name(row[0])
        result_text = self._normalize(row[1])
        if not self._is_indicator_name(name) or not result_text:
            return None
        if is_non_indicator_name(name):
            return None

        value_match = re.match(r"^\s*[↑↓HhLl*]*\s*([-+]?\d+(?:[.,]\d+)?)", result_text)
        if value_match is None:
            return None
        value = self._decimal(value_match.group(1))
        unit = self._pdf_result_unit(result_text[value_match.end() :])
        reference_text = row[3] if len(row) > 3 else ""
        reference_low, reference_high = self._pdf_reference_values(reference_text)

        matched = self._matched_pdf_indicator(name)
        if matched is not None:
            code, standard_name, standard_unit, _ = matched
            return IndicatorInput(
                code=code,
                name=standard_name,
                value=value,
                unit=standard_unit or unit,
                referenceLow=reference_low,
                referenceHigh=reference_high,
            )

        return IndicatorInput(
            code="unrecognized_" + hashlib.sha1(name.encode()).hexdigest()[:10],
            name=name,
            value=value,
            unit=unit or "index",
            referenceLow=reference_low,
            referenceHigh=reference_high,
        )

    def _clean_pdf_name(self, value: str) -> str:
        """Clean a table-cell name without treating digits in analyte names as ranges.

        The generic OCR cleaner deliberately rejects text containing a numeric range. That is
        useful for flattened OCR lines, but wrong for native table cells such as CA24-2. In an
        electronic PDF the first column is already the analyte column, so its name can be kept.
        """
        cleaned = ROW_NUMBER_PATTERN.sub("", value, count=1).strip()
        cleaned = self._correct_common_ocr_errors(cleaned)
        return cleaned if re.search(r"[\u4e00-\u9fa5A-Za-z]", cleaned) else ""

    def _matched_pdf_indicator(self, name: str) -> tuple[str, str, str, str] | None:
        """Conservatively normalize a native PDF analyte name.

        Fuzzy matching is unsafe for structured rows: ``MCV`` was previously matched to ``RBC``
        and ``AST/ALT`` to ``ALT``. Those collisions overwrite otherwise valid rows. Match only
        an exact full name or an exact code enclosed in parentheses; unknown names remain intact.
        """
        corrected = self._correct_common_ocr_errors(name).strip()
        normalized_name = re.sub(r"\s+", "", corrected).casefold()
        parenthesized_codes = {
            re.sub(r"\s+", "", token).casefold()
            for token in re.findall(r"[（(]([^）)]+)[）)]", corrected)
        }
        matches = [
            (code, standard_name, standard_unit, alias)
            for code, standard_name, standard_unit, aliases in self.INDICATORS
            for alias in aliases
            if re.sub(r"\s+", "", alias).casefold() == normalized_name
            or re.sub(r"\s+", "", alias).casefold() in parenthesized_codes
        ]
        return max(matches, key=lambda item: len(item[3])) if matches else None

    @staticmethod
    def _pdf_result_unit(value: str) -> str:
        cleaned = re.sub(r"[↑↓*]", "", value)
        cleaned = re.sub(r"\s+", "", cleaned).strip("()（）")
        match = re.search(
            r"(?:10\^\d{1,2}/[A-Za-z]+|[%A-Za-zμu]+(?:/[A-Za-z0-9.²^]+)?)",
            cleaned,
        )
        if match is None:
            return ""
        unit = match.group(0)
        replacements = {
            "umol": "μmol",
            "ug": "μg",
            "mmg": "mmHg",
            "dpm/mmo": "dpm/mmol",
        }
        for source, target in replacements.items():
            unit = unit.replace(source, target)
        return unit

    def _pdf_reference_values(self, value: str) -> tuple[Decimal | None, Decimal | None]:
        normalized = self._normalize(value).replace("--", "-")
        range_match = RANGE_PATTERN.search(normalized)
        if range_match:
            return self._decimal(range_match.group(1)), self._decimal(range_match.group(2))
        upper_match = re.search(r"(?:≤|<|不超过)\s*([-+]?\d+(?:[.,]\d+)?)", normalized)
        if upper_match:
            return None, self._decimal(upper_match.group(1))
        lower_match = re.search(r"(?:≥|>|不少于)\s*([-+]?\d+(?:[.,]\d+)?)", normalized)
        if lower_match:
            return self._decimal(lower_match.group(1)), None
        return None, None

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
                    # Header anchoring is a fallback for skewed rows. A visually grouped row is
                    # stronger evidence and must not be overwritten by a fallback row that may
                    # have absorbed a neighbouring value or a footer timestamp.
                    parsed.setdefault(item.code or item.name, item)
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
        name_token = self._clean_name(ordered[name_index].text) if name_index is not None else ""
        matched = self._matched_indicator(name_token, exact=True) if name_token else None
        search_start = name_index + 1 if name_index is not None else 0
        value_index = next(
            (
                index
                for index, token in enumerate(ordered[search_start:], start=search_start)
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
        parsed_value = self._decimal(value_token)
        row_text = " ".join(token.text for token in ordered)
        if matched is not None:
            code, standard_name, standard_unit, _ = matched
            return IndicatorInput(
                code=code,
                name=standard_name,
                value=parsed_value,
                unit=self._unit(row_text, standard_unit),
                referenceLow=reference_low,
                referenceHigh=reference_high,
            )
        unit = self._unit(row_text, "")
        inferred = self._infer_common_panel_indicator(
            name_token, parsed_value, unit, reference_low, reference_high
        )
        if inferred is not None:
            code, standard_name, standard_unit = inferred
            return IndicatorInput(
                code=code,
                name=standard_name,
                value=parsed_value,
                unit=standard_unit,
                referenceLow=reference_low,
                referenceHigh=reference_high,
            )
        if not name_token or not unit or reference_low is None or reference_high is None:
            return None
        return IndicatorInput(
            code="unrecognized_" + hashlib.sha1(name_token.encode()).hexdigest()[:10],
            name=name_token,
            value=parsed_value,
            unit=unit,
            referenceLow=reference_low,
            referenceHigh=reference_high,
        )

    def _infer_common_panel_indicator(
        self,
        name: str,
        value: Decimal,
        unit: str,
        reference_low: Decimal | None,
        reference_high: Decimal | None,
    ) -> tuple[str, str, str] | None:
        """Recover a few strongly identifiable rows whose short Chinese name was missed.

        This is deliberately limited to combined value/unit/range fingerprints that are
        distinctive in a routine chemistry panel. It does not guess from a value alone.
        """
        normalized_unit = self._normalize_unit(unit)
        # A recurring PP-OCR artifact on photographed liver panels turns “谷草转氨酶” into
        # “各” and “U/L” into “T/n”. The combination is specific enough to recover AST, but
        # the damaged reference interval is intentionally left empty rather than fabricated.
        if (
            name == "各"
            and normalized_unit == self._normalize_unit("T/n")
            and Decimal("5") <= value <= Decimal("80")
        ):
            return "ast", "天门冬氨酸氨基转移酶", "U/L"
        if reference_low is None or reference_high is None:
            return None
        weak_name = not name or len(name) <= 2 or name in {"离子", "各"}
        if not weak_name:
            return None
        if normalized_unit == self._normalize_unit("mmol/L"):
            if (
                Decimal("1.8") <= reference_low <= Decimal("2.3")
                and Decimal("2.4") <= reference_high <= Decimal("2.8")
                and Decimal("1.5") <= value <= Decimal("3.5")
            ):
                return "calcium", "钙", "mmol/L"
            if (
                Decimal("80") <= reference_low <= Decimal("110")
                and Decimal("100") <= reference_high <= Decimal("125")
                and Decimal("70") <= value <= Decimal("150")
            ):
                return "chloride", "氯", "mmol/L"
        if normalized_unit == self._normalize_unit("U/L") and (
            Decimal("10") <= reference_low <= Decimal("20")
            and Decimal("30") <= reference_high <= Decimal("50")
            and Decimal("5") <= value <= Decimal("80")
        ):
            return "ast", "天门冬氨酸氨基转移酶", "U/L"
        return None

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
        if is_non_indicator_name(compact):
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
        if (
            "密度脂蛋白胆固醇" in corrected
            and "高密度脂蛋白胆固醇" not in corrected
            and "低密度脂蛋白胆固醇" not in corrected
        ):
            corrected = corrected.replace("密度脂蛋白胆固醇", "低密度脂蛋白胆固醇")
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
    MIN_PARTIAL_ACCEPTED = 8

    def validate(
        self,
        indicators: list[IndicatorInput],
        text_confidence: Decimal,
        raw_lines: list[str],
        *,
        trusted_structure: bool = False,
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
            or (
                rejected_count > 0
                and rejected_ratio >= self.MAX_REJECTED_RATIO
                and len(accepted) < self.MIN_PARTIAL_ACCEPTED
            )
            or (not trusted_structure and unknown_ratio > self.MAX_UNKNOWN_RATIO)
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
        if is_non_indicator_name(item.name):
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
        if self._medically_plausible(item):
            score += 0.15
        # Do not reward an OCR candidate merely because it makes the result look "normal".
        # Abnormal laboratory values are valid evidence. Equal candidates keep their input
        # order, and PaddleOcrService deliberately supplies layout-aware rows first.
        return score, 0.0

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
            findings=[],
            raw_lines=lines,
            warnings=["当前开发环境使用结构化演示识别结果，请人工核对后确认。"],
        )


class PaddleOcrService(OcrService):
    MIN_IMAGE_EDGE = 2000
    MAX_IMAGE_EDGE = 4200
    IMAGE_COLUMN_RETRY_MIN_ASPECT = 1.18
    IMAGE_COLUMN_RETRY_MIN_INDICATORS = 24
    IMAGE_COLUMN_OVERLAP_RATIO = 0.06
    PDF_RENDER_SCALE = 2.6
    PDF_NATIVE_TEXT_CONFIDENCE = Decimal("0.9900")

    def __init__(self, qwen_client: QwenOcrClient | None = None) -> None:
        self.parser = IndicatorRowParser()
        self.validator = OcrQualityValidator()
        self.qwen_client = qwen_client or QwenOcrClient()

    def recognize(self, request: OcrRecognizeRequest) -> OcrRecognizeData:
        if self.qwen_client.enabled:
            try:
                return self._recognize_with_qwen(request)
            except QwenOcrError:
                local_result = self._recognize_locally(request)
                return local_result.model_copy(
                    update={
                        "warnings": [
                            "阿里云增强识别暂不可用，已自动切换本地识别。",
                            *local_result.warnings,
                        ]
                    }
                )
        return self._recognize_locally(request)

    def _recognize_locally(self, request: OcrRecognizeRequest) -> OcrRecognizeData:
        if not request.download_url:
            raise ValueError("PaddleOCR requires a signed downloadUrl")
        path = self._download(request)
        temporary_paths: list[Path] = []
        native_lines: list[str] = []
        lines: list[str] = []
        scores: list[float] = []
        boxes: list[tuple[float, float, float, float]] = []
        table_indicators: list[IndicatorInput] = []
        pdf_findings: list[OcrFinding] = []
        is_pdf = request.mime_type.lower() == "application/pdf" or path.suffix.lower() == ".pdf"
        try:
            if is_pdf:
                # Electronic health reports usually contain a reliable text layer. Extract it
                # first, then render every page at high resolution for scanned pages and tables.
                # This avoids asking a vision OCR model to rediscover text that already exists.
                native_lines = self._extract_pdf_text(path)
                table_indicators = self.parser.parse_pdf_tables(path)
                pdf_findings = self.parser.parse_pdf_findings(path)
                # A substantial native table is already lossless and column-aligned. Avoid a
                # slow second OCR pass over every page; image-only or sparse PDFs still fall back.
                if len(table_indicators) < 8:
                    for page_path in self._render_pdf_pages(path):
                        temporary_paths.append(page_path)
                        prepared_path = self._prepare_image(page_path, "image/png")
                        if prepared_path != page_path:
                            temporary_paths.append(prepared_path)
                        page_lines, page_scores, page_boxes = self._recognize_file(prepared_path)
                        lines.extend(page_lines)
                        scores.extend(page_scores)
                        boxes.extend(page_boxes)
            else:
                prepared_path = self._prepare_image(path, request.mime_type)
                if prepared_path != path:
                    temporary_paths.append(prepared_path)
                lines, scores, boxes = self._recognize_file(prepared_path)
        finally:
            path.unlink(missing_ok=True)
            for temporary_path in temporary_paths:
                temporary_path.unlink(missing_ok=True)

        layout_indicators = self.parser.parse_layout(lines, boxes)
        sequential_indicators = self.parser.parse(lines)
        text_confidence = (
            Decimal(str(round(sum(scores) / len(scores), 4))) if scores else Decimal("0")
        )
        if table_indicators:
            text_confidence = self.PDF_NATIVE_TEXT_CONFIDENCE
        # Prefer layout candidates when two parsers produce equally plausible rows. Sequential
        # OCR order frequently interleaves the left and right halves of a two-column report.
        # The validator still rejects unsafe layout rows and can fall back to sequential results.
        ocr_quality = self.validator.validate(
            [*layout_indicators, *sequential_indicators], text_confidence, lines
        )
        quality = ocr_quality
        if is_pdf:
            native_quality = self.validator.validate(
                self.parser.parse(native_lines),
                self.PDF_NATIVE_TEXT_CONFIDENCE if native_lines else Decimal("0"),
                native_lines,
            )
            quality = self._prefer_pdf_quality(native_quality, ocr_quality)
            if table_indicators:
                table_quality = self.validator.validate(
                    table_indicators,
                    self.PDF_NATIVE_TEXT_CONFIDENCE,
                    native_lines,
                    trusted_structure=True,
                )
                quality = self._prefer_pdf_quality(table_quality, quality)

        warnings: list[str] = []
        if not quality.indicators:
            warnings.append("已识别报告文字，但没有提取到可安全用于评估的检验指标。")
        if text_confidence < Decimal("0.80"):
            warnings.append("报告文字清晰度偏低，已启用低可信数据保护。")
        warnings.extend(quality.warnings)
        return OcrRecognizeData(
            engine=(
                "PDF-table-text-plus-PaddleOCR-3.7.0/PP-OCRv6-small-enhanced"
                if is_pdf
                else "PaddleOCR-3.7.0/PP-OCRv6-small-enhanced"
            ),
            status=quality.status,
            confidence=quality.confidence,
            indicators=quality.indicators,
            findings=pdf_findings,
            raw_lines=native_lines if native_lines else lines,
            warnings=warnings,
        )

    def _recognize_with_qwen(self, request: OcrRecognizeRequest) -> OcrRecognizeData:
        if not request.download_url:
            raise ValueError("Qwen3.5-OCR requires a signed downloadUrl")
        path = self._download(request)
        rendered_paths: list[Path] = []
        native_lines: list[str] = []
        local_indicators: list[IndicatorInput] = []
        local_findings: list[OcrFinding] = []
        cloud_indicators: list[IndicatorInput] = []
        cloud_findings: list[OcrFinding] = []
        cloud_lines: list[str] = []
        image_column_recovery_used = False
        is_pdf = request.mime_type.lower() == "application/pdf" or path.suffix.lower() == ".pdf"
        try:
            if is_pdf:
                native_lines = self._extract_pdf_text(path)
                local_indicators = self.parser.parse_pdf_tables(path)
                local_findings = self.parser.parse_pdf_findings(path)
                rendered_paths = self._render_pdf_pages(
                    path, max_pages=self.qwen_client.settings.max_pages
                )
                cloud_outputs = self.qwen_client.recognize_images(rendered_paths)
            else:
                cloud_outputs = self.qwen_client.recognize_images([path])
                cloud_indicators, cloud_findings, cloud_lines = self._parse_qwen_outputs(
                    cloud_outputs
                )
                # A full landscape photo can be downscaled enough for the model to follow only
                # one half of a two-column laboratory table. Retry only an incomplete image with
                # two overlapping, higher-resolution column views. PDF handling above remains
                # unchanged and continues to use native text plus page rendering.
                if self._should_retry_image_columns(path, len(cloud_indicators)):
                    try:
                        rendered_paths = self._split_image_columns(path)
                        split_outputs = self.qwen_client.recognize_images(rendered_paths)
                        split_indicators, split_findings, split_lines = (
                            self._parse_qwen_outputs(split_outputs)
                        )
                        if split_indicators or split_findings:
                            cloud_indicators = self._merge_indicators(
                                split_indicators, cloud_indicators
                            )
                            cloud_findings = self._merge_findings(
                                split_findings, cloud_findings
                            )
                            cloud_lines = [*split_lines, *cloud_lines]
                            image_column_recovery_used = True
                    except (OSError, ValueError, QwenOcrError):
                        # Enhancement is best-effort. Keep the successful full-image result when
                        # creating or recognizing the additional views fails.
                        pass
        finally:
            path.unlink(missing_ok=True)
            for rendered_path in rendered_paths:
                rendered_path.unlink(missing_ok=True)

        if is_pdf:
            cloud_indicators, cloud_findings, cloud_lines = self._parse_qwen_outputs(
                cloud_outputs
            )
        if not cloud_indicators and not cloud_findings:
            raise QwenOcrError("Qwen3.5-OCR did not return structured medical content")

        # Qwen is the primary source for images. For an electronic PDF, the native text layer is
        # lossless and carries the source category, order and wording. Do not append model-created
        # categories or rewritten rows to that source sequence; Qwen remains the fallback for
        # scanned/image-only documents whose native finding list is empty.
        indicators = self._merge_indicators(cloud_indicators, local_indicators)
        if native_lines:
            indicators = self._merge_indicators(indicators, self.parser.parse(native_lines))
        findings = self._merge_findings(local_findings or cloud_findings, [])
        quality = self.validator.validate(
            indicators,
            Decimal("0.9700"),
            cloud_lines or native_lines,
            trusted_structure=True,
        )
        warnings = list(quality.warnings)
        if not quality.indicators:
            warnings.insert(0, "已识别报告内容，但没有提取到可安全用于评估的数值指标。")
        return OcrRecognizeData(
            engine=(
                "Qwen3.5-OCR+PDF-native-validation"
                if is_pdf
                else (
                    "Qwen3.5-OCR+multi-column-recovery"
                    if image_column_recovery_used
                    else "Qwen3.5-OCR"
                )
            ),
            status=quality.status,
            confidence=quality.confidence,
            indicators=quality.indicators,
            findings=findings,
            raw_lines=cloud_lines,
            warnings=warnings,
        )

    def _should_retry_image_columns(self, path: Path, indicator_count: int) -> bool:
        if indicator_count >= self.IMAGE_COLUMN_RETRY_MIN_INDICATORS:
            return False
        try:
            with Image.open(path) as source:
                image = ImageOps.exif_transpose(source)
                return image.width >= image.height * self.IMAGE_COLUMN_RETRY_MIN_ASPECT
        except (OSError, ValueError):
            return False

    def _split_image_columns(self, path: Path) -> list[Path]:
        """Create left-to-right overlapping views without modifying the source image."""

        output_paths: list[Path] = []
        with Image.open(path) as source:
            image = ImageOps.exif_transpose(source).convert("RGB")
            midpoint = image.width // 2
            overlap = max(24, round(image.width * self.IMAGE_COLUMN_OVERLAP_RATIO))
            boxes = (
                (0, 0, min(image.width, midpoint + overlap), image.height),
                (max(0, midpoint - overlap), 0, image.width, image.height),
            )
            for box in boxes:
                view = image.crop(box)
                with tempfile.NamedTemporaryFile(delete=False, suffix=".png") as target:
                    view.save(target, format="PNG", optimize=True)
                    output_paths.append(Path(target.name))
        return output_paths

    def _parse_qwen_outputs(
        self, outputs: list[str]
    ) -> tuple[list[IndicatorInput], list[OcrFinding], list[str]]:
        indicators: list[IndicatorInput] = []
        findings: list[OcrFinding] = []
        raw_lines: list[str] = []
        for output in outputs:
            document = extract_qwen_document(output)
            for table in document.tables:
                indicators.extend(self._qwen_table_indicators(table))
                raw_lines.extend("\t".join(cell for cell in row if cell) for row in table)
            for raw_line in document.lines:
                line = raw_line.strip().strip("` ")
                line = re.sub(r"^[-*•]\s*", "", line)
                if not line or line.lower() in {"tsv", "text"}:
                    continue
                raw_lines.append(line)
                cells = line.split("\t")
                if len(cells) < 2 and "|" in line:
                    cells = [cell.strip() for cell in line.strip("|").split("|")]
                cells = [cell.strip() for cell in cells]
                kind = cells[0].replace(" ", "")
                if kind == "指标" and len(cells) >= 3:
                    item = self._qwen_indicator(cells)
                    if item is not None:
                        indicators.append(item)
                elif kind == "发现" and len(cells) >= 4:
                    result = " ".join(cell for cell in cells[3:] if cell).strip()
                    if result:
                        findings.append(
                            OcrFinding(
                                section=cells[1] or "体检结果",
                                item=cells[2] or "检查结论",
                                result=result,
                            )
                        )

        # Model output can occasionally lose separators. Reuse the proven local row parser as a
        # lossless recovery path, while still treating the cloud transcription as the source.
        indicators = self._merge_indicators(indicators, self.parser.parse(raw_lines))
        indicators = [
            item for item in indicators if not self._qwen_metadata_name(item.name)
        ]
        return indicators, self._merge_findings(findings, []), raw_lines

    def _qwen_table_indicators(self, table: list[list[str]]) -> list[IndicatorInput]:
        """Map Qwen HTML tables without relying on a hospital-specific column order."""

        indicators: list[IndicatorInput] = []
        columns: dict[str, int] = {}
        for row in table:
            cells = [re.sub(r"\s+", " ", cell).strip() for cell in row]
            if not any(cells):
                continue
            detected = self._qwen_header_columns(cells)
            if "name" in detected and "value" in detected:
                columns = detected
                continue
            item = self._qwen_mapped_table_indicator(cells, columns)
            if item is None:
                # Some HTML tables omit a header or use a conventional four-column layout.
                item = self.parser._parse_pdf_table_row(cells)
            if item is not None:
                indicators.append(item)
        return self._merge_indicators(indicators, [])

    @staticmethod
    def _qwen_header_columns(cells: list[str]) -> dict[str, int]:
        columns: dict[str, int] = {}
        for index, value in enumerate(cells):
            compact = re.sub(r"[\s:：]", "", value)
            if compact in HEADER_NAMES or compact in {"名称", "中文名称", "指标名称"}:
                columns["name"] = index
            elif compact in HEADER_VALUES or compact in {"检查结果", "测量值"}:
                columns["value"] = index
            elif compact in HEADER_UNITS:
                columns["unit"] = index
            elif compact in HEADER_REFERENCES:
                columns["reference"] = index
            elif compact in {"参考下限", "下限"}:
                columns["low"] = index
            elif compact in {"参考上限", "上限"}:
                columns["high"] = index
            elif compact in {"异常标记", "标志", "提示", "状态"}:
                columns["flag"] = index
        return columns

    def _qwen_mapped_table_indicator(
        self, cells: list[str], columns: dict[str, int]
    ) -> IndicatorInput | None:
        if not columns or max(columns.values(), default=-1) >= len(cells):
            return None
        name = cells[columns["name"]] if "name" in columns else ""
        value = cells[columns["value"]] if "value" in columns else ""
        if not name or not value:
            return None
        unit = cells[columns["unit"]] if "unit" in columns else ""
        low = cells[columns["low"]] if "low" in columns else ""
        high = cells[columns["high"]] if "high" in columns else ""
        if "reference" in columns:
            reference_low, reference_high = self.parser._pdf_reference_values(
                cells[columns["reference"]]
            )
            low = str(reference_low) if reference_low is not None else low
            high = str(reference_high) if reference_high is not None else high
        flag = cells[columns["flag"]] if "flag" in columns else ""
        return self._qwen_indicator(["指标", name, value, unit, low, high, flag])

    def _qwen_indicator(self, cells: list[str]) -> IndicatorInput | None:
        name = cells[1].strip(" ：:")
        if not name or self._qwen_metadata_name(name):
            return None
        value = self._qwen_decimal(cells[2])
        if value is None:
            return None
        unit = cells[3] if len(cells) > 3 and cells[3] else "未注明"
        reference_low = self._qwen_decimal(cells[4]) if len(cells) > 4 else None
        reference_high = self._qwen_decimal(cells[5]) if len(cells) > 5 else None
        matched = self.parser._matched_pdf_indicator(name)
        if matched is not None:
            code, standard_name, standard_unit, _ = matched
            return IndicatorInput(
                code=code,
                name=standard_name,
                value=value,
                unit=unit if unit != "未注明" else standard_unit,
                referenceLow=reference_low,
                referenceHigh=reference_high,
            )
        return IndicatorInput(
            code="unrecognized_" + hashlib.sha1(name.encode()).hexdigest()[:10],
            name=name,
            value=value,
            unit=unit,
            referenceLow=reference_low,
            referenceHigh=reference_high,
        )

    @staticmethod
    def _qwen_metadata_name(name: str) -> bool:
        return is_non_indicator_name(name)

    @staticmethod
    def _qwen_decimal(value: str) -> Decimal | None:
        match = NUMBER_PATTERN.search(value.replace("，", ","))
        if match is None:
            return None
        try:
            return Decimal(match.group(0).replace(",", "."))
        except InvalidOperation:
            return None

    @staticmethod
    def _merge_indicators(
        primary: list[IndicatorInput], supplemental: list[IndicatorInput]
    ) -> list[IndicatorInput]:
        merged: dict[str, IndicatorInput] = {
            item.code or item.name.strip(): item for item in primary
        }
        for item in supplemental:
            merged.setdefault(item.code or item.name.strip(), item)
        return list(merged.values())

    @staticmethod
    def _merge_findings(
        primary: list[OcrFinding], supplemental: list[OcrFinding]
    ) -> list[OcrFinding]:
        merged: list[OcrFinding] = []
        exact_seen: set[tuple[str, str, str]] = set()
        source_items: set[tuple[str, str]] = set()
        for item in primary:
            if is_non_finding_metadata_name(item.item):
                continue
            section = item.section.strip()
            name = item.item.strip()
            result = item.result.strip()
            exact_key = (section, name, result)
            if exact_key in exact_seen:
                continue
            exact_seen.add(exact_key)
            source_items.add((section, name))
            merged.append(item)
        for item in supplemental:
            if is_non_finding_metadata_name(item.item):
                continue
            section = item.section.strip()
            name = item.item.strip()
            result = item.result.strip()
            exact_key = (section, name, result)
            # Different source summary lines are meaningful; other cloud rewrites must not
            # duplicate or replace the original PDF item/content.
            if exact_key in exact_seen or (
                name not in {"小结", "小结：", "检查小结", "检查小结："}
                and (section, name) in source_items
            ):
                continue
            exact_seen.add(exact_key)
            source_items.add((section, name))
            merged.append(item)
        return merged

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

    def _extract_pdf_text(self, path: Path) -> list[str]:
        """Return the native text layer of an electronic PDF, preserving page reading order."""
        try:
            import pypdfium2 as pdfium  # type: ignore[import-untyped]

            document = pdfium.PdfDocument(str(path))
            try:
                text_parts: list[str] = []
                for page_index in range(len(document)):
                    page = document[page_index]
                    text_page = page.get_textpage()
                    text_parts.append(text_page.get_text_range())
                return self._normalise_pdf_lines(text_parts)
            finally:
                document.close()
        except Exception:
            # A scanned, encrypted or malformed report may have no readable text layer. The
            # high-resolution image route remains available and is deliberately not blocked.
            return []

    def _render_pdf_pages(self, path: Path, max_pages: int | None = None) -> list[Path]:
        """Render every PDF page for OCR fallback, including multi-page electronic reports."""
        try:
            import pypdfium2 as pdfium

            document = pdfium.PdfDocument(str(path))
            rendered_paths: list[Path] = []
            try:
                page_count = len(document) if max_pages is None else min(len(document), max_pages)
                for page_index in range(page_count):
                    page = document[page_index]
                    bitmap = page.render(scale=self.PDF_RENDER_SCALE)
                    image = bitmap.to_pil()
                    with tempfile.NamedTemporaryFile(delete=False, suffix=".png") as target:
                        image.save(target.name, format="PNG", optimize=True)
                        rendered_paths.append(Path(target.name))
                return rendered_paths
            finally:
                document.close()
        except Exception as exc:
            raise ValueError("Unable to render PDF pages for OCR") from exc

    @staticmethod
    def _normalise_pdf_lines(text_parts: list[str]) -> list[str]:
        lines: list[str] = []
        for text in text_parts:
            for raw_line in re.split(r"[\r\n]+", text):
                line = re.sub(r"\s+", " ", raw_line).strip()
                if line:
                    lines.append(line)
        return lines

    @staticmethod
    def _prefer_pdf_quality(
        native_quality: OcrQualityResult, ocr_quality: OcrQualityResult
    ) -> OcrQualityResult:
        """Use the safer result with the most structurally valid indicators.

        PDF text is preferred on ties because it is source text rather than a visual guess.
        Both candidates pass the same clinical-structure validator before this comparison.
        """
        native_rank = (
            native_quality.status == "SUCCESS",
            len(native_quality.indicators),
            native_quality.confidence,
        )
        ocr_rank = (
            ocr_quality.status == "SUCCESS",
            len(ocr_quality.indicators),
            ocr_quality.confidence,
        )
        return native_quality if native_rank >= ocr_rank else ocr_quality

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
            page_texts = self._as_list(data.get("rec_texts"))
            page_scores = self._as_list(data.get("rec_scores"))
            page_boxes = self._as_list(data.get("rec_boxes"))
            # Paddle occasionally emits an empty recognition result while retaining its score
            # and box. Filtering text independently shifts every following text onto the wrong
            # box, which is especially destructive for side-by-side laboratory tables. Keep all
            # three arrays aligned by their original index and filter each OCR token atomically.
            for index, raw_text in enumerate(page_texts):
                text = str(raw_text).strip()
                box = self.parser._box(page_boxes[index]) if index < len(page_boxes) else None
                if not text or box is None:
                    continue
                score = float(page_scores[index]) if index < len(page_scores) else 0.0
                lines.append(text)
                scores.append(score)
                boxes.append(box)
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
