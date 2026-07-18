from typing import Generic, TypeVar

from pydantic import BaseModel, ConfigDict, Field

T = TypeVar("T")


class RaykModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True)


class ApiResponse(RaykModel, Generic[T]):
    code: int = 0
    message: str = "success"
    request_id: str = Field(alias="requestId")
    timestamp: int
    data: T


class HealthData(RaykModel):
    status: str
    service: str
    version: str
