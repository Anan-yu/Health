import logging
import time

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.router import ocr_service, router
from app.core.middleware import RequestIdMiddleware
from app.core.request_context import get_request_id
from app.ocr.service import PaddleOcrService
from app.schemas.common import ApiResponse, HealthData

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)

app = FastAPI(
    title="Rayk AI Service",
    version="0.1.0",
    description="模拟OCR、指标标准化与演示健康评估服务，不构成医学诊断。",
)
app.add_middleware(RequestIdMiddleware)
app.include_router(router)


@app.on_event("startup")
def warm_up_ocr() -> None:
    if isinstance(ocr_service, PaddleOcrService):
        ocr_service.warm_up()


@app.get("/health", response_model=ApiResponse[HealthData])
def health() -> ApiResponse[HealthData]:
    return ApiResponse(
        request_id=get_request_id(),
        timestamp=int(time.time() * 1000),
        data=HealthData(status="UP", service="rayk-ai", version="0.1.0"),
    )


@app.exception_handler(RequestValidationError)
async def validation_error(_: Request, exc: RequestValidationError) -> JSONResponse:
    safe_errors = [
        {key: value for key, value in error.items() if key not in {"input", "url"}}
        for error in exc.errors()
    ]
    return JSONResponse(
        status_code=422,
        content={
            "code": 40001,
            "message": "请求参数校验失败",
            "requestId": get_request_id(),
            "timestamp": int(time.time() * 1000),
            "data": {"errors": safe_errors},
        },
    )


@app.exception_handler(Exception)
async def unhandled_error(_: Request, __: Exception) -> JSONResponse:
    logging.getLogger("rayk.ai.error").exception("unhandled AI service error")
    return JSONResponse(
        status_code=500,
        content={
            "code": 50000,
            "message": "AI服务内部错误",
            "requestId": get_request_id(),
            "timestamp": int(time.time() * 1000),
            "data": None,
        },
    )
