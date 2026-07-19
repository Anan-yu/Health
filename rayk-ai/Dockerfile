FROM python:3.12.10-slim AS runtime

ARG PIP_INDEX_URL=https://pypi.tuna.tsinghua.edu.cn/simple

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    TZ=Asia/Shanghai

WORKDIR /app
RUN groupadd --system rayk && useradd --system --gid rayk --home-dir /app rayk
RUN apt-get update \
    && apt-get install -y --no-install-recommends libgl1 libglib2.0-0 libgomp1 \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt ./
RUN pip install --no-cache-dir --index-url "$PIP_INDEX_URL" --upgrade pip \
    && pip install --no-cache-dir --index-url "$PIP_INDEX_URL" -r requirements.txt
COPY app ./app
RUN mkdir -p /app/.paddlex /app/.cache \
    && chown -R rayk:rayk /app
USER rayk
EXPOSE 8000
HEALTHCHECK --interval=15s --timeout=5s --retries=5 CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')"
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
