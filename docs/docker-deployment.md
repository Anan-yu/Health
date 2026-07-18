# Docker 部署

Docker Desktop 的磁盘镜像位置应由用户在设置中改到 E 盘。本机已确认 `CustomWslDistroDir` 为 `E:\DockerData\DockerDesktop\DockerDesktopWSL`。项目使用命名卷 `rayk_mysql_data`、`rayk_redis_data`、`rayk_minio_data` 和 `rayk_logs_data`，它们会随 Docker 磁盘镜像落在 E 盘。

禁止手动搬运 Docker WSL 文件；禁止 `docker compose down -v` 和会删除镜像/卷的 prune 命令。停止服务只执行普通 `down`。

项目内已为 Maven 配置阿里云公共仓库、为 pip 配置清华 PyPI、为 npm 配置 npmmirror，不修改宿主机全局配置。基础容器镜像可通过 `.env` 的 `MYSQL_IMAGE`、`REDIS_IMAGE`、`MINIO_IMAGE`、`MINIO_MC_IMAGE`、`NGINX_IMAGE` 替换为用户可信的国内代理地址。
