import json
from decimal import Decimal
from pathlib import Path

import httpx
from PIL import Image

from app.ocr.qwen import (
    QwenOcrClient,
    QwenOcrPageResult,
    QwenOcrSettings,
    extract_qwen_document,
)
from app.ocr.service import OcrQualityValidator, PaddleOcrService
from app.schemas.indicator import IndicatorInput
from app.schemas.ocr import OcrRecognizeRequest


def test_qwen_client_sends_base64_image_and_medical_transcription_prompt(
    tmp_path: Path,
) -> None:
    captured: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["authorization"] = request.headers["Authorization"]
        body = request.read().decode("utf-8")
        captured["body"] = body
        return httpx.Response(
            200,
            json={
                "choices": [
                    {
                        "message": {
                            "content": "指标\t总胆固醇\t5.99\tmmol/L\t0\t5.17\t偏高"
                        }
                    }
                ]
            },
        )

    image_path = tmp_path / "report.png"
    Image.new("RGB", (160, 120), "white").save(image_path)
    settings = QwenOcrSettings(
        enabled=True,
        api_key="test-key",
        workspace_id="workspace-test",
        base_url="https://example.test/compatible-mode/v1",
        model="qwen3.7-flash-2026-07-15",
        timeout_seconds=30,
        max_pixels=16_000_000,
        max_pages=5,
    )
    client = QwenOcrClient(settings, transport=httpx.MockTransport(handler))

    result = client.recognize_images([image_path])

    assert result == ["指标\t总胆固醇\t5.99\tmmol/L\t0\t5.17\t偏高"]
    assert captured["authorization"] == "Bearer test-key"
    assert '"model":"qwen3.7-flash-2026-07-15"' in str(captured["body"])
    assert "data:image/jpeg;base64," in str(captured["body"])
    assert "不要把姓名" in str(captured["body"])


def test_qwen_ocr_defaults_to_qwen37_flash(monkeypatch) -> None:
    monkeypatch.delenv("QWEN_OCR_MODEL", raising=False)
    monkeypatch.setenv("QWEN_OCR_CONCURRENCY", "999")

    settings = QwenOcrSettings.from_env()

    assert settings.model == "qwen3.7-flash-2026-07-15"
    assert settings.fallback_model == "qwen3.5-ocr"
    assert settings.concurrency == 50


def test_qwen_page_request_can_override_model_for_pdf_fallback(tmp_path: Path) -> None:
    captured_models: list[str] = []

    def handler(request: httpx.Request) -> httpx.Response:
        payload = json.loads(request.read().decode("utf-8"))
        captured_models.append(payload["model"])
        return httpx.Response(
            200,
            json={
                "choices": [
                    {
                        "message": {
                            "content": "指标\t总胆固醇\t5.99\tmmol/L\t0\t5.17\t偏高"
                        }
                    }
                ]
            },
        )

    image_path = tmp_path / "page.png"
    Image.new("RGB", (160, 120), "white").save(image_path)
    settings = QwenOcrSettings(
        True,
        "test-key",
        "",
        "https://example.test/compatible-mode/v1",
        "qwen3.7-flash-2026-07-15",
        30,
        16_000_000,
        5,
    )

    result = QwenOcrClient(
        settings, transport=httpx.MockTransport(handler)
    ).recognize_images_best_effort([image_path], model="qwen3.5-ocr")

    assert captured_models == ["qwen3.5-ocr"]
    assert result[0].model == "qwen3.5-ocr"
    assert result[0].text


def test_qwen_rows_are_normalized_and_metadata_is_rejected() -> None:
    service = PaddleOcrService(
        qwen_client=QwenOcrClient(
            QwenOcrSettings(False, "", "", "", "qwen3.7-flash-2026-07-15", 30, 16_000_000, 5)
        )
    )
    indicators, findings, raw_lines = service._parse_qwen_outputs(
        [
            "\n".join(
                [
                    "指标\t总胆固醇\t5.99\tmmol/L\t0\t5.17\t偏高",
                    "指标\t姓名 高建刚 男 60岁\t2026\t\t7\t25\t",
                    "发现\t腹部超声\t肝脏\t脂肪肝声像",
                    "发现\t基本信息\t门诊号\t科室 29 病区住院号",
                    "发现\t基本信息\t床位号\t检查号 26070770709 仪器型号",
                ]
            )
        ]
    )

    assert len(indicators) == 1
    assert indicators[0].code == "total_cholesterol"
    assert indicators[0].value == Decimal("5.99")
    assert indicators[0].reference_high == Decimal("5.17")
    assert len(findings) == 1
    assert findings[0].section == "腹部超声"
    assert findings[0].result == "脂肪肝声像"
    assert len(raw_lines) == 5


def test_qwen_generic_rows_recover_visible_project_name_and_result() -> None:
    service = PaddleOcrService(
        qwen_client=QwenOcrClient(
            QwenOcrSettings(False, "", "", "", "qwen3.7-flash-2026-07-15", 30, 16_000_000, 5)
        )
    )

    _, findings, _ = service._parse_qwen_outputs(
        [
            "\n".join(
                [
                    "发现\t阴性/阳性 酶联免疫法\t原文\t乙肝病毒表面抗体",
                    "发现\t阴性/阳性 酶联免疫法\t原文\t阴性(-) 0.962",
                    "发现\t阴性 酶联免疫法\t原文\t乙肝病毒E抗原",
                    "发现\t阴性 酶联免疫法\t原文\t阳性(+)(+) 0.154",
                ]
            )
        ]
    )

    assert [(finding.item, finding.result) for finding in findings] == [
        ("乙肝病毒表面抗体", "阴性(-) 0.962"),
        ("乙肝病毒E抗原", "阳性(+)(+) 0.154"),
    ]


def test_qwen_json_rows_preserve_numeric_and_narrative_content() -> None:
    service = PaddleOcrService(
        qwen_client=QwenOcrClient(
            QwenOcrSettings(False, "", "", "", "qwen3.7-flash-2026-07-15", 30, 16_000_000, 5)
        )
    )
    output = {
        "categories": [
            {
                "categoryName": "生化检验",
                "items": [
                    {
                        "projectName": "总胆固醇",
                        "result": "5.99",
                        "unit": "mmol/L",
                        "referenceRange": "0-5.17",
                    },
                    {
                        "type": "finding",
                        "projectName": "检查小结",
                        "result": "血脂异常，建议结合临床随访。",
                    },
                ],
            }
        ]
    }

    indicators, findings, raw_lines = service._parse_qwen_outputs([json.dumps(output)])

    assert [item.code for item in indicators] == ["total_cholesterol"]
    assert indicators[0].reference_low == Decimal("0")
    assert indicators[0].reference_high == Decimal("5.17")
    assert findings[0].section == "生化检验"
    assert findings[0].item == "检查小结"
    assert "血脂异常" in findings[0].result
    assert any(line.startswith("发现\t生化检验") for line in raw_lines)


def test_qwen_unstructured_transcription_is_kept_for_manual_review() -> None:
    service = PaddleOcrService(
        qwen_client=QwenOcrClient(
            QwenOcrSettings(False, "", "", "", "qwen3.7-flash-2026-07-15", 30, 16_000_000, 5)
        )
    )

    findings = service._qwen_unstructured_findings(
        ["胸部CT", "双肺散在小结节，建议结合临床观察。", "报告日期：2026-08-14"]
    )

    assert len(findings) == 1
    assert findings[0].item == "原文"
    assert "小结节" in findings[0].result


def test_qwen_client_falls_back_to_public_endpoint_when_workspace_rejects(
    tmp_path: Path,
) -> None:
    requested_hosts: list[str] = []

    def handler(request: httpx.Request) -> httpx.Response:
        requested_hosts.append(request.url.host)
        if request.url.host.startswith("workspace-test."):
            return httpx.Response(400, json={"message": "invalid workspace"})
        return httpx.Response(
            200,
            json={"choices": [{"message": {"content": "无结构化内容"}}]},
        )

    image_path = tmp_path / "report.png"
    Image.new("RGB", (160, 120), "white").save(image_path)
    settings = QwenOcrSettings(
        enabled=True,
        api_key="test-key",
        workspace_id="workspace-test",
        base_url="",
        model="qwen3.7-flash-2026-07-15",
        timeout_seconds=30,
        max_pixels=16_000_000,
        max_pages=5,
    )

    result = QwenOcrClient(
        settings, transport=httpx.MockTransport(handler)
    ).recognize_images([image_path])

    assert result == ["无结构化内容"]
    assert requested_hosts == [
        "workspace-test.cn-beijing.maas.aliyuncs.com",
        "dashscope.aliyuncs.com",
    ]


def test_qwen_html_tables_are_extracted_and_mapped_by_headers() -> None:
    output = """
    ```html
    <html><body>
      <h2>生化检验</h2>
      <table>
        <tr><th>检验项目</th><th>检验结果</th><th>单位</th><th>参考范围</th></tr>
        <tr><td>总胆固醇</td><td>5.99 ↑</td><td>mmol/L</td><td>0-5.17</td></tr>
        <tr><td>低密度脂蛋白胆固醇</td><td>3.62</td><td>mmol/L</td><td>0-3.36</td></tr>
      </table>
    </body></html>
    ```
    """
    document = extract_qwen_document(output)
    assert document.lines == ["生化检验"]
    assert len(document.tables) == 1

    service = PaddleOcrService(
        qwen_client=QwenOcrClient(
            QwenOcrSettings(False, "", "", "", "qwen3.7-flash-2026-07-15", 30, 16_000_000, 5)
        )
    )
    indicators, _, raw_lines = service._parse_qwen_outputs([output])

    assert {item.code for item in indicators} == {
        "total_cholesterol",
        "ldl",
    }
    assert indicators[0].reference_high is not None
    assert "生化检验" in raw_lines


def test_qwen_html_metadata_table_is_not_treated_as_indicators() -> None:
    output = """
    <table>
      <tr><th>项目</th><th>结果</th><th>单位</th><th>参考范围</th></tr>
      <tr><td>姓名</td><td>某某</td><td></td><td></td></tr>
      <tr><td>打印日期</td><td>2026-08-01</td><td></td><td></td></tr>
    </table>
    """
    service = PaddleOcrService(
        qwen_client=QwenOcrClient(
            QwenOcrSettings(False, "", "", "", "qwen3.7-flash-2026-07-15", 30, 16_000_000, 5)
        )
    )

    indicators, _, _ = service._parse_qwen_outputs([output])

    assert indicators == []


def test_qwen_html_preserves_nested_imaging_narrative_and_summary() -> None:
    output = """
    <html><body>
      <div>腹部CT</div>
      <div><p>影像表现：肝脏边缘欠光整，肝右叶见小结节高密度影。</p>
      <p>检查小结：肝硬化可能，建议结合临床随访。</p></div>
    </body></html>
    """
    document = extract_qwen_document(output)

    assert document.lines == [
        "腹部CT",
        "影像表现：肝脏边缘欠光整，肝右叶见小结节高密度影。",
        "检查小结：肝硬化可能，建议结合临床随访。",
    ]
    service = PaddleOcrService(
        qwen_client=QwenOcrClient(
            QwenOcrSettings(False, "", "", "", "qwen3.7-flash-2026-07-15", 30, 16_000_000, 5)
        )
    )
    indicators, findings, _ = service._parse_qwen_outputs([output])

    assert indicators == []
    assert [finding.item for finding in findings] == ["影像表现", "检查小结"]
    assert findings[0].section == "腹部CT"
    assert "肝硬化可能" in findings[1].result


def test_narrative_only_image_waits_for_confirmation_instead_of_retry(
    tmp_path: Path,
) -> None:
    class FakeQwenClient:
        def recognize_images(self, paths: list[Path]) -> list[str]:
            del paths
            return [
                "<div>胸部CT</div><div>影像表现：双肺散在小结节，建议结合临床观察。</div>"
            ]

    source_path = tmp_path / "ct.png"
    Image.new("RGB", (1200, 1800), "white").save(source_path)
    service = PaddleOcrService(qwen_client=FakeQwenClient())  # type: ignore[arg-type]
    service._download = lambda _request: source_path  # type: ignore[method-assign]

    result = service._recognize_with_qwen(
        OcrRecognizeRequest(
            file_id="image-ct",
            object_name="ct.png",
            mime_type="image/png",
            download_url="https://example.test/ct.png",
        )
    )

    assert result.status == "WAITING_CONFIRMATION"
    assert result.indicators == []
    assert result.findings
    assert result.confidence == Decimal("0.7000")
    assert "人工核对" in result.warnings[0]


def test_pdf_uses_ocr_fallback_only_for_failed_qwen_pages(tmp_path: Path) -> None:
    class FakeQwenClient:
        def __init__(self) -> None:
            self.settings = QwenOcrSettings(
                True,
                "test-key",
                "",
                "",
                "qwen3.7-flash-2026-07-15",
                30,
                16_000_000,
                5,
                3,
                "qwen3.5-ocr",
            )
            self.calls: list[tuple[str, list[str]]] = []

        def recognize_images_best_effort(
            self, paths: list[Path], *, model: str | None = None
        ) -> list[QwenOcrPageResult]:
            model_name = model or self.settings.model
            self.calls.append((model_name, [path.name for path in paths]))
            if model_name == self.settings.model:
                return [
                    QwenOcrPageResult(
                        path=paths[0],
                        text="指标\t空腹血糖\t6.20\tmmol/L\t3.90\t6.10\t偏高",
                        model=model_name,
                    ),
                    QwenOcrPageResult(
                        path=paths[1],
                        text=None,
                        model=None,
                        error="qwen3.7-flash-2026-07-15:QwenOcrError",
                    ),
                ]
            return [
                QwenOcrPageResult(
                    path=paths[0],
                    text="指标\t总胆固醇\t5.99\tmmol/L\t0\t5.17\t偏高",
                    model=model_name,
                )
            ]

    source_path = tmp_path / "report.pdf"
    source_path.write_bytes(b"not-a-real-pdf")
    page_paths = [tmp_path / "page-1.png", tmp_path / "page-2.png"]
    for page_path in page_paths:
        Image.new("RGB", (800, 1000), "white").save(page_path)

    client = FakeQwenClient()
    service = PaddleOcrService(qwen_client=client)  # type: ignore[arg-type]
    service._download = lambda _request: source_path  # type: ignore[method-assign]
    service._extract_pdf_text = lambda _path: []  # type: ignore[method-assign]
    service.parser.parse_pdf_tables = lambda _path: []  # type: ignore[method-assign]
    service.parser.parse_pdf_findings = lambda _path: []  # type: ignore[method-assign]
    service._render_pdf_pages = (  # type: ignore[method-assign]
        lambda _path, max_pages=None: page_paths[:max_pages]
        if max_pages is not None
        else page_paths
    )

    result = service._recognize_with_qwen(
        OcrRecognizeRequest(
            file_id="pdf-1",
            object_name="report.pdf",
            mime_type="application/pdf",
            download_url="https://example.test/report.pdf",
        )
    )

    assert client.calls == [
        ("qwen3.7-flash-2026-07-15", ["page-1.png", "page-2.png"]),
        ("qwen3.5-ocr", ["page-2.png"]),
    ]
    assert result.engine == "qwen3.7-flash-2026-07-15>qwen3.5-ocr+PDF-native-validation"
    assert {item.code for item in result.indicators} == {
        "fasting_glucose",
        "total_cholesterol",
    }
    assert "重试 1 页" in result.warnings[0]


def test_landscape_image_recovers_both_columns_without_using_pdf_path(
    tmp_path: Path,
) -> None:
    class FakeQwenClient:
        def __init__(self) -> None:
            self.calls: list[list[Path]] = []

        def recognize_images(self, paths: list[Path]) -> list[str]:
            self.calls.append(paths)
            right = "\n".join(
                [
                    "指标\t谷丙转氨酶\t14.4\tU/L\t9\t50\t正常",
                    "指标\t谷草转氨酶\t19.5\tU/L\t15\t40\t正常",
                    "指标\t总胆红素\t22.7\tumol/L\t3\t22\t偏高",
                    "指标\t白蛋白\t48.9\tg/L\t40\t55\t正常",
                ]
            )
            if len(self.calls) == 1:
                return [right]
            left = "\n".join(
                [
                    "指标\t钾\t4.6\tmmol/L\t3.5\t5.5\t正常",
                    "指标\t钠\t140\tmmol/L\t137\t147\t正常",
                    "指标\t葡萄糖\t4.92\tmmol/L\t3.91\t6.1\t正常",
                    "指标\t肌酐\t75.4\tumol/L\t44\t110\t正常",
                ]
            )
            return [left, right]

    source_path = tmp_path / "two-column.png"
    Image.new("RGB", (2048, 1536), "white").save(source_path)
    client = FakeQwenClient()
    service = PaddleOcrService(qwen_client=client)  # type: ignore[arg-type]
    service._download = lambda _request: source_path  # type: ignore[method-assign]

    result = service._recognize_with_qwen(
        OcrRecognizeRequest(
            file_id="image-1",
            object_name="two-column.png",
            mime_type="image/png",
            download_url="https://example.test/two-column.png",
        )
    )

    assert result.engine == "qwen3.7-flash-2026-07-15+multi-column-recovery"
    assert len(result.indicators) == 8
    assert {item.code for item in result.indicators} >= {
        "fasting_glucose",
        "creatinine",
        "alt",
        "ast",
        "total_bilirubin",
        "albumin",
    }
    assert [len(paths) for paths in client.calls] == [1, 2]


def test_final_quality_gate_removes_metadata_reintroduced_by_pdf_fallback() -> None:
    rows = [
        IndicatorInput(
            code="unrecognized_exam_date",
            name="体检日期",
            value=Decimal("2026"),
            unit="6",
            referenceLow=Decimal("7"),
            referenceHigh=Decimal("25"),
        ),
        IndicatorInput(
            code="unrecognized_phone",
            name="咨询电话",
            value=Decimal("39"),
            unit="6",
            referenceLow=Decimal("6"),
            referenceHigh=Decimal("2398188"),
        ),
        IndicatorInput(
            code="unrecognized_height",
            name="身高 160 cm",
            value=Decimal("10"),
            unit="0",
            referenceLow=Decimal("0"),
            referenceHigh=Decimal("300"),
        ),
        IndicatorInput(
            code="unrecognized_identity",
            name="姓名 高建刚 男 60 岁",
            value=Decimal("202"),
            unit="6",
            referenceLow=Decimal("7"),
            referenceHigh=Decimal("25"),
        ),
        IndicatorInput(
            code="total_cholesterol",
            name="总胆固醇",
            value=Decimal("5.99"),
            unit="mmol/L",
            referenceLow=Decimal("0"),
            referenceHigh=Decimal("5.17"),
        ),
    ]

    result = OcrQualityValidator().validate(
        rows,
        Decimal("0.97"),
        ["structured result"],
        trusted_structure=True,
    )

    assert [item.name for item in result.indicators] == ["总胆固醇"]
