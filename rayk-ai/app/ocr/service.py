from abc import ABC, abstractmethod
from decimal import Decimal

from app.schemas.indicator import IndicatorInput
from app.schemas.ocr import OcrRecognizeData, OcrRecognizeRequest


class OcrService(ABC):
    @abstractmethod
    def recognize(self, request: OcrRecognizeRequest) -> OcrRecognizeData: ...


class MockOcrService(OcrService):
    def recognize(self, request: OcrRecognizeRequest) -> OcrRecognizeData:
        indicators = [
            IndicatorInput(
                code="fasting_glucose",
                name="空腹血糖",
                value=Decimal("6.20"),
                unit="mmol/L",
                referenceLow=Decimal("3.90"),
                referenceHigh=Decimal("6.10"),
            ),
            IndicatorInput(
                code="hba1c",
                name="糖化血红蛋白",
                value=Decimal("5.80"),
                unit="%",
                referenceLow=Decimal("4.00"),
                referenceHigh=Decimal("6.00"),
            ),
            IndicatorInput(
                code="crp",
                name="C反应蛋白",
                value=Decimal("4.10"),
                unit="mg/L",
                referenceHigh=Decimal("3.00"),
            ),
        ]
        return OcrRecognizeData(
            engine="MOCK_OCR_1.0",
            status="WAITING_CONFIRMATION",
            confidence=0.96,
            indicators=indicators,
            warnings=["当前为模拟OCR结果，请人工核对后确认。"],
        )


class PaddleOcrAdapter(OcrService):
    def recognize(self, request: OcrRecognizeRequest) -> OcrRecognizeData:
        raise NotImplementedError("PaddleOCR adapter is reserved for the next phase")

