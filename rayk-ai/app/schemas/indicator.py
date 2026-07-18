from decimal import Decimal

from pydantic import Field

from app.schemas.common import RaykModel


class IndicatorInput(RaykModel):
    code: str | None = None
    name: str = Field(min_length=1, max_length=100)
    value: Decimal
    unit: str = Field(min_length=1, max_length=30)
    reference_low: Decimal | None = Field(default=None, alias="referenceLow")
    reference_high: Decimal | None = Field(default=None, alias="referenceHigh")


class NormalizationRequest(RaykModel):
    indicators: list[IndicatorInput]


class NormalizedIndicator(RaykModel):
    code: str
    standard_name: str = Field(alias="standardName")
    value: Decimal
    unit: str
    recognized: bool


class NormalizationData(RaykModel):
    indicators: list[NormalizedIndicator]
