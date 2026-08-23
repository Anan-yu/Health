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


class ReleaseData(RaykModel):
    release_id: str = Field(alias="releaseId")
    git_commit: str = Field(alias="gitCommit")
    git_dirty: bool = Field(alias="gitDirty")
    build_time: str = Field(alias="buildTime")
    database_migration: str = Field(alias="databaseMigration")
    frontend_h5_sha256: str = Field(alias="frontendH5Sha256")
    frontend_mp_weixin_dev_sha256: str = Field(alias="frontendMpWeixinDevSha256")
    frontend_mp_weixin_prod_lan_sha256: str = Field(alias="frontendMpWeixinProdLanSha256")
