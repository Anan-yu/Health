from decimal import Decimal
from pathlib import Path

import httpx
from PIL import Image

from app.ocr.qwen import QwenOcrClient, QwenOcrSettings, extract_qwen_document
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
        model="qwen3.5-ocr",
        timeout_seconds=30,
        max_pixels=16_000_000,
        max_pages=5,
    )
    client = QwenOcrClient(settings, transport=httpx.MockTransport(handler))

    result = client.recognize_images([image_path])

    assert result == ["指标\t总胆固醇\t5.99\tmmol/L\t0\t5.17\t偏高"]
    assert captured["authorization"] == "Bearer test-key"
    assert '"model":"qwen3.5-ocr"' in str(captured["body"])
    assert "data:image/jpeg;base64," in str(captured["body"])
    assert "不要把姓名" in str(captured["body"])


def test_qwen_rows_are_normalized_and_metadata_is_rejected() -> None:
    service = PaddleOcrService(
        qwen_client=QwenOcrClient(
            QwenOcrSettings(False, "", "", "", "qwen3.5-ocr", 30, 16_000_000, 5)
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
        model="qwen3.5-ocr",
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
            QwenOcrSettings(False, "", "", "", "qwen3.5-ocr", 30, 16_000_000, 5)
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
            QwenOcrSettings(False, "", "", "", "qwen3.5-ocr", 30, 16_000_000, 5)
        )
    )

    indicators, _, _ = service._parse_qwen_outputs([output])

    assert indicators == []


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

    assert result.engine == "Qwen3.5-OCR+multi-column-recovery"
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
