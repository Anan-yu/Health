import json
import re
from dataclasses import dataclass, replace
from pathlib import Path
from typing import Any

from sklearn.feature_extraction.text import TfidfVectorizer  # type: ignore[import-untyped]
from sklearn.metrics.pairwise import linear_kernel  # type: ignore[import-untyped]

from app.schemas.assessment import AssessmentRequest, ModelResult
from app.schemas.followup import FollowupAdjustmentRequest

KNOWLEDGE_BASE_VERSION = "ZHIYU_MEDICAL_KB_2.0.0"

_EMPTY_VALUES = {"", "UNKNOWN", "NONE", "NO_DATA", None}
_MODEL_TOPICS: dict[str, frozenset[str]] = {
    "GLUCOSE_METABOLISM": frozenset({"糖代谢", "糖尿病", "高血糖"}),
    "LIPID_CARDIOVASCULAR": frozenset({"血脂", "心血管", "高血压"}),
    "CHRONIC_INFLAMMATION": frozenset({"炎症", "免疫"}),
    "LIVER_METABOLIC": frozenset({"肝脏", "胆道", "脂肪肝"}),
    "KIDNEY_ELECTROLYTE": frozenset({"肾脏", "电解质", "高尿酸"}),
    "HEMATOLOGY_ANEMIA": frozenset({"血液", "贫血"}),
    "THYROID_HORMONE": frozenset({"甲状腺", "内分泌"}),
    "BODY_COMPOSITION": frozenset({"体重", "身体成分", "运动"}),
    "HPA_ADRENAL": frozenset({"睡眠", "压力", "恢复"}),
    "NUTRITION_MICRONUTRIENT": frozenset({"营养", "微量元素", "维生素"}),
    "GUT_BARRIER": frozenset({"消化", "肠道", "饮食"}),
    "MENTAL_EMOTIONAL": frozenset({"情绪", "心理", "压力"}),
}


def _present(value: Any) -> bool:
    if isinstance(value, list):
        return bool(value)
    if isinstance(value, str):
        return value.strip().upper() not in _EMPTY_VALUES
    return value not in _EMPTY_VALUES


def _text(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, list):
        return " ".join(_text(item) for item in value)
    if isinstance(value, dict):
        return " ".join(f"{key} {_text(item)}" for key, item in value.items())
    return str(value)


def _compact_tokens(value: str) -> frozenset[str]:
    return frozenset(
        token.lower() for token in re.findall(r"[a-zA-Z][a-zA-Z0-9_.-]*|[\u4e00-\u9fff]{2,}", value)
    )


@dataclass(frozen=True)
class MedicalKnowledgeReference:
    reference_id: str
    title: str
    guidance: str
    source_name: str
    source_url: str
    document_version: str
    published_at: str
    authority_level: str
    evidence_type: str
    section: str
    topics: frozenset[str] = frozenset()
    model_codes: frozenset[str] = frozenset()
    indicator_codes: frozenset[str] = frozenset()
    context_fields: frozenset[str] = frozenset()
    keywords: frozenset[str] = frozenset()
    applicable_population: tuple[str, ...] = ()
    contraindications: tuple[str, ...] = ()
    always_include: bool = False
    retrieval_score: float = 0.0
    matched_by: tuple[str, ...] = ()

    @property
    def searchable_text(self) -> str:
        return " ".join(
            [
                self.title,
                self.guidance,
                self.section,
                *sorted(self.topics),
                *sorted(self.model_codes),
                *sorted(self.indicator_codes),
                *sorted(self.context_fields),
                *sorted(self.keywords),
                *self.applicable_population,
                *self.contraindications,
            ]
        )

    def to_prompt_dict(self) -> dict[str, Any]:
        return {
            "evidenceId": self.reference_id,
            "referenceId": self.reference_id,
            "title": self.title,
            "organization": self.source_name,
            "documentVersion": self.document_version,
            "publishedAt": self.published_at,
            "authorityLevel": self.authority_level,
            "evidenceType": self.evidence_type,
            "section": self.section,
            "content": self.guidance,
            "applicablePopulation": list(self.applicable_population),
            "contraindications": list(self.contraindications),
            "sourceUrl": self.source_url,
            "knowledgeBaseVersion": KNOWLEDGE_BASE_VERSION,
            "retrieval": {
                "score": round(self.retrieval_score, 4),
                "matchedBy": list(self.matched_by),
            },
        }


@dataclass(frozen=True)
class KnowledgeQueryPlan:
    query_text: str
    model_codes: frozenset[str]
    indicator_codes: frozenset[str]
    abnormal_indicator_codes: frozenset[str]
    context_fields: frozenset[str]
    topics: frozenset[str]
    tokens: frozenset[str]


class MedicalKnowledgeRetriever:
    """Local hybrid RAG retriever over a medically reviewed, versioned evidence corpus."""

    def __init__(self, data_path: Path | None = None) -> None:
        self.data_path = data_path or Path(__file__).with_name("medical_knowledge_v2.json")
        self.references = self._load_references(self.data_path)
        self._vectorizer = TfidfVectorizer(
            analyzer="char_wb",
            ngram_range=(2, 4),
            min_df=1,
            sublinear_tf=True,
            norm="l2",
        )
        self._search_matrix = self._vectorizer.fit_transform(
            item.searchable_text for item in self.references
        )

    def retrieve(
        self,
        request: AssessmentRequest,
        results: list[ModelResult],
        limit: int = 10,
    ) -> list[MedicalKnowledgeReference]:
        return self._retrieve(self._plan_assessment_query(request, results), limit=limit)

    def retrieve_for_followup(
        self,
        request: FollowupAdjustmentRequest,
        limit: int = 6,
    ) -> list[MedicalKnowledgeReference]:
        return self._retrieve(self._plan_followup_query(request), limit=limit)

    def _retrieve(
        self,
        plan: KnowledgeQueryPlan,
        *,
        limit: int,
    ) -> list[MedicalKnowledgeReference]:
        bounded_limit = max(2, min(limit, len(self.references)))
        query_vector = self._vectorizer.transform([plan.query_text or "健康管理"])
        semantic_scores = linear_kernel(query_vector, self._search_matrix).ravel().tolist()
        ranked: list[MedicalKnowledgeReference] = []
        always: list[MedicalKnowledgeReference] = []

        for index, reference in enumerate(self.references):
            if reference.always_include:
                always.append(reference)
                continue

            matched_by: list[str] = []
            structured_score = 0.0
            model_hits = reference.model_codes & plan.model_codes
            abnormal_hits = reference.indicator_codes & plan.abnormal_indicator_codes
            indicator_hits = reference.indicator_codes & plan.indicator_codes
            context_hits = reference.context_fields & plan.context_fields
            topic_hits = reference.topics & plan.topics
            keyword_hits = {
                keyword
                for keyword in reference.keywords
                if keyword.lower() in plan.query_text.lower() or keyword.lower() in plan.tokens
            }

            if model_hits:
                structured_score += len(model_hits) * 8.0
                matched_by.extend(f"模型:{item}" for item in sorted(model_hits))
            if abnormal_hits:
                structured_score += len(abnormal_hits) * 7.0
                matched_by.extend(f"异常指标:{item}" for item in sorted(abnormal_hits))
            if indicator_hits:
                structured_score += len(indicator_hits - abnormal_hits) * 3.0
                matched_by.extend(f"指标:{item}" for item in sorted(indicator_hits - abnormal_hits))
            if context_hits:
                structured_score += len(context_hits) * 2.0
                matched_by.extend(f"档案:{item}" for item in sorted(context_hits))
            if topic_hits:
                structured_score += len(topic_hits) * 4.0
                matched_by.extend(f"主题:{item}" for item in sorted(topic_hits))
            if keyword_hits:
                structured_score += min(len(keyword_hits), 5) * 2.0
                matched_by.extend(f"关键词:{item}" for item in sorted(keyword_hits)[:5])

            semantic_score = float(semantic_scores[index])
            total_score = structured_score + semantic_score * 10.0
            if total_score < 1.25:
                continue
            if semantic_score >= 0.08:
                matched_by.append("中文语义相似")
            ranked.append(
                replace(
                    reference,
                    retrieval_score=total_score,
                    matched_by=tuple(dict.fromkeys(matched_by)),
                )
            )

        ranked.sort(
            key=lambda item: (
                -item.retrieval_score,
                item.authority_level,
                item.reference_id,
            )
        )
        selected_count = max(0, bounded_limit - len(always))
        selected = [*always[:1], *ranked[:selected_count], *always[1:]]
        return self._deduplicate(selected)[:bounded_limit]

    @staticmethod
    def _plan_assessment_query(
        request: AssessmentRequest,
        results: list[ModelResult],
    ) -> KnowledgeQueryPlan:
        indicator_codes = frozenset(item.code for item in request.indicators if item.code)
        abnormal_codes = frozenset(
            item.code
            for item in request.indicators
            if item.code
            and (
                (item.reference_low is not None and item.value < item.reference_low)
                or (item.reference_high is not None and item.value > item.reference_high)
            )
        )
        evaluated_models = frozenset(
            item.model_code for item in results if item.status == "EVALUATED"
        )
        context_payload = (
            request.patient_context.model_dump(exclude_none=True)
            if request.patient_context is not None
            else {}
        )
        context_fields = frozenset(key for key, value in context_payload.items() if _present(value))
        topics = frozenset(
            topic
            for model_code in evaluated_models
            for topic in _MODEL_TOPICS.get(model_code, frozenset())
        )
        query_parts = [
            "医学健康评估",
            *sorted(topics),
            *[
                f"{item.code or ''} {item.name} {item.value} {item.unit}"
                for item in request.indicators
            ],
            *[f"{item.section} {item.item} {item.result}" for item in request.findings],
            *[
                f"{item.model_code} {item.model_name} {_text(item.evidence)} "
                f"{_text(item.recommendations)}"
                for item in results
                if item.status == "EVALUATED"
            ],
            _text(context_payload),
        ]
        query_text = " ".join(part for part in query_parts if part)
        return KnowledgeQueryPlan(
            query_text=query_text,
            model_codes=evaluated_models,
            indicator_codes=indicator_codes,
            abnormal_indicator_codes=abnormal_codes,
            context_fields=context_fields,
            topics=topics,
            tokens=_compact_tokens(query_text),
        )

    @staticmethod
    def _plan_followup_query(request: FollowupAdjustmentRequest) -> KnowledgeQueryPlan:
        context_payload = (
            request.patient_context.model_dump(exclude_none=True)
            if request.patient_context is not None
            else {}
        )
        context_fields = frozenset(key for key, value in context_payload.items() if _present(value))
        action_text = " ".join(
            f"{item.section} {item.action} {item.note or ''}" for item in request.actions
        )
        query_text = " ".join(
            [
                "健康随访 行为调整 营养 运动 睡眠 情绪",
                _text(context_payload),
                request.feedback or "",
                action_text,
            ]
        )
        inferred_topics = {
            topic
            for topic in (
                "营养",
                "运动",
                "睡眠",
                "压力",
                "情绪",
                "高血压",
                "糖尿病",
                "高血糖",
                "血脂",
                "高尿酸",
                "肾脏",
            )
            if topic in query_text
        }
        return KnowledgeQueryPlan(
            query_text=query_text,
            model_codes=frozenset(),
            indicator_codes=frozenset(),
            abnormal_indicator_codes=frozenset(),
            context_fields=context_fields,
            topics=frozenset(inferred_topics),
            tokens=_compact_tokens(query_text),
        )

    @staticmethod
    def _deduplicate(
        references: list[MedicalKnowledgeReference],
    ) -> list[MedicalKnowledgeReference]:
        result: list[MedicalKnowledgeReference] = []
        seen_ids: set[str] = set()
        seen_sections: set[tuple[str, str]] = set()
        for reference in references:
            section_key = (reference.source_url, reference.section)
            if reference.reference_id in seen_ids or (
                section_key in seen_sections and not reference.always_include
            ):
                continue
            result.append(reference)
            seen_ids.add(reference.reference_id)
            seen_sections.add(section_key)
        return result

    @staticmethod
    def _load_references(path: Path) -> tuple[MedicalKnowledgeReference, ...]:
        raw = json.loads(path.read_text(encoding="utf-8"))
        if raw.get("knowledgeBaseVersion") != KNOWLEDGE_BASE_VERSION:
            raise ValueError("Medical knowledge base version mismatch")
        references: list[MedicalKnowledgeReference] = []
        seen: set[str] = set()
        for item in raw.get("documents", []):
            reference_id = str(item["evidenceId"]).strip()
            if not reference_id or reference_id in seen:
                raise ValueError("Medical knowledge evidence IDs must be unique")
            seen.add(reference_id)
            references.append(
                MedicalKnowledgeReference(
                    reference_id=reference_id,
                    title=str(item["title"]).strip(),
                    guidance=str(item["guidance"]).strip(),
                    source_name=str(item["sourceName"]).strip(),
                    source_url=str(item["sourceUrl"]).strip(),
                    document_version=str(item["documentVersion"]).strip(),
                    published_at=str(item["publishedAt"]).strip(),
                    authority_level=str(item["authorityLevel"]).strip(),
                    evidence_type=str(item["evidenceType"]).strip(),
                    section=str(item["section"]).strip(),
                    topics=frozenset(item.get("topics", [])),
                    model_codes=frozenset(item.get("modelCodes", [])),
                    indicator_codes=frozenset(item.get("indicatorCodes", [])),
                    context_fields=frozenset(item.get("contextFields", [])),
                    keywords=frozenset(item.get("keywords", [])),
                    applicable_population=tuple(item.get("applicablePopulation", [])),
                    contraindications=tuple(item.get("contraindications", [])),
                    always_include=bool(item.get("alwaysInclude", False)),
                )
            )
        if len(references) < 10:
            raise ValueError("Medical knowledge corpus is unexpectedly small")
        return tuple(references)
