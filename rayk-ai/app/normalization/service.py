import re
from dataclasses import dataclass

from app.schemas.indicator import IndicatorInput, NormalizedIndicator


@dataclass(frozen=True)
class IndicatorDefinition:
    code: str
    name: str
    unit: str


class IndicatorNormalizationService:
    _definitions = {
        "空腹血糖": IndicatorDefinition("fasting_glucose", "空腹血糖", "mmol/L"),
        "GLU": IndicatorDefinition("fasting_glucose", "空腹血糖", "mmol/L"),
        "空腹胰岛素": IndicatorDefinition("fasting_insulin", "空腹胰岛素", "μIU/mL"),
        "糖化血红蛋白": IndicatorDefinition("hba1c", "糖化血红蛋白", "%"),
        "HBA1C": IndicatorDefinition("hba1c", "糖化血红蛋白", "%"),
        "甘油三酯": IndicatorDefinition("triglyceride", "甘油三酯", "mmol/L"),
        "高密度脂蛋白": IndicatorDefinition("hdl", "高密度脂蛋白", "mmol/L"),
        "C反应蛋白": IndicatorDefinition("crp", "C反应蛋白", "mg/L"),
        "促甲状腺激素": IndicatorDefinition("tsh", "促甲状腺激素", "mIU/L"),
        "游离甲状腺素": IndicatorDefinition("ft4", "游离甲状腺素", "pmol/L"),
    }

    def normalize(self, indicator: IndicatorInput) -> NormalizedIndicator:
        key = re.sub(r"\s+", "", indicator.name).upper()
        definition = self._definitions.get(key)
        if definition is None and indicator.code:
            definition = next(
                (item for item in self._definitions.values() if item.code == indicator.code), None
            )
        if definition is None:
            return NormalizedIndicator(
                code=indicator.code or "unrecognized",
                standard_name=indicator.name,
                value=indicator.value,
                unit=indicator.unit,
                recognized=False,
            )
        return NormalizedIndicator(
            code=definition.code,
            standard_name=definition.name,
            value=indicator.value,
            unit=definition.unit,
            recognized=True,
        )
