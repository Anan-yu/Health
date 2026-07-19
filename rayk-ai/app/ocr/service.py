import hashlib
import os
import re
import tempfile
from abc import ABC, abstractmethod
from collections.abc import Iterable
from decimal import Decimal
from functools import lru_cache
from pathlib import Path
from typing import Any

import httpx

from app.schemas.indicator import IndicatorInput
from app.schemas.ocr import OcrRecognizeData, OcrRecognizeRequest

MAX_FILE_SIZE = 20 * 1024 * 1024
NUMBER_PATTERN = re.compile(r"[-+]?\d+(?:[.,]\d+)?")


class OcrService(ABC):
    @abstractmethod
    def recognize(self, request: OcrRecognizeRequest) -> OcrRecognizeData: ...


class IndicatorRowParser:
    INDICATORS: tuple[tuple[str, str, str, tuple[str, ...]], ...] = (
        ("fasting_glucose", "空腹血糖", "mmol/L", ("空腹血糖", "葡萄糖", "GLU")),
        ("hba1c", "糖化血红蛋白", "%", ("糖化血红蛋白", "糖化血色素", "HbA1c")),
        ("crp", "C反应蛋白", "mg/L", ("超敏C反应蛋白", "C反应蛋白", "hs-CRP", "CRP")),
        ("total_cholesterol", "总胆固醇", "mmol/L", ("总胆固醇", "胆固醇", "TC")),
        ("triglyceride", "甘油三酯", "mmol/L", ("甘油三酯", "TG")),
        ("hdl", "高密度脂蛋白胆固醇", "mmol/L", ("高密度脂蛋白胆固醇", "HDL-C", "HDL")),
        ("ldl", "低密度脂蛋白胆固醇", "mmol/L", ("低密度脂蛋白胆固醇", "LDL-C", "LDL")),
        ("alt", "丙氨酸氨基转移酶", "U/L", ("丙氨酸氨基转移酶", "谷丙转氨酶", "ALT")),
        ("ast", "天门冬氨酸氨基转移酶", "U/L", ("天门冬氨酸氨基转移酶", "谷草转氨酶", "AST")),
        ("creatinine", "肌酐", "μmol/L", ("血肌酐", "肌酐", "CREA", "Cr")),
        ("uric_acid", "尿酸", "μmol/L", ("尿酸", "UA")),
        ("tsh", "促甲状腺激素", "mIU/L", ("促甲状腺激素", "TSH")),
        ("vitamin_d", "25羟维生素D", "ng/mL", ("25羟维生素D", "25-OH-D", "维生素D")),
    )

    def parse(self, lines: Iterable[str]) -> list[IndicatorInput]:
        parsed: dict[str, IndicatorInput] = {}
        normalized_lines = [self._normalize(line) for line in lines if line.strip()]
        for line in normalized_lines:
            known = self._parse_known(line)
            if known is not None:
                parsed[known.code or known.name] = known
                continue
            generic = self._parse_generic(line)
            if generic is not None:
                parsed[generic.code or generic.name] = generic
        return list(parsed.values())

    def _parse_known(self, line: str) -> IndicatorInput | None:
        lowered = line.casefold()
        for code, standard_name, standard_unit, aliases in self.INDICATORS:
            alias = next((item for item in aliases if item.casefold() in lowered), None)
            if alias is None:
                continue
            alias_index = lowered.index(alias.casefold())
            tail = line[alias_index + len(alias) :]
            numbers = [self._decimal(value) for value in NUMBER_PATTERN.findall(tail)]
            if not numbers:
                continue
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
            return candidates[0], candidates[1]
        return None, None

    def _unit(self, tail: str, fallback: str) -> str:
        units = re.findall(r"[%A-Za-zμµ]+(?:/[A-Za-z]+)?", tail)
        ignored = {"H", "L", "N", "HIGH", "LOW"}
        return next((unit for unit in units if unit.upper() not in ignored), fallback)

    def _normalize(self, value: str) -> str:
        return re.sub(r"\s+", " ", value.replace("：", " ").replace(":", " ")).strip()

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
    def __init__(self) -> None:
        self.parser = IndicatorRowParser()

    def recognize(self, request: OcrRecognizeRequest) -> OcrRecognizeData:
        if not request.download_url:
            raise ValueError("PaddleOCR requires a signed downloadUrl")
        path = self._download(request)
        try:
            lines, scores = self._recognize_file(path)
        finally:
            path.unlink(missing_ok=True)
        indicators = self.parser.parse(lines)
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
        if not content or len(content) > MAX_FILE_SIZE:
            raise ValueError("OCR input file is empty or exceeds 20MB")
        suffix = ".pdf" if request.mime_type == "application/pdf" else ".png"
        if request.mime_type == "image/jpeg":
            suffix = ".jpg"
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as target:
            target.write(content)
            return Path(target.name)

    def _recognize_file(self, path: Path) -> tuple[list[str], list[float]]:
        lines: list[str] = []
        scores: list[float] = []
        for result in self._pipeline().predict(str(path)):
            payload = getattr(result, "json", result)
            if callable(payload):
                payload = payload()
            if not isinstance(payload, dict):
                continue
            data = payload.get("res", payload)
            lines.extend(
                str(item).strip()
                for item in self._as_list(data.get("rec_texts"))
                if str(item).strip()
            )
            scores.extend(float(item) for item in self._as_list(data.get("rec_scores")))
        return lines, scores

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
