# 腾讯云语音合成接入说明

系统使用腾讯云语音合成 `TextToVoice` 接口，为个人健康中心提供两类语音提醒：吃饭提醒与睡觉提醒。

## 安全边界

- `SecretId`、`SecretKey` 只保存在部署机器的 `.env`，由 Java 服务读取。
- 小程序不持有云端密钥，也不直接请求腾讯云。
- 合成音频写入私有 MinIO，客户端只能通过登录态访问本人生成的音频。
- 服务日志只记录腾讯云错误码和请求 ID，不记录密钥、签名或完整响应。

## 本地配置

在项目根目录 `.env` 中填写以下配置，`.env` 已被 Git 忽略：

```dotenv
TENCENT_TTS_ENABLED=true
TENCENT_TTS_APP_ID=腾讯云账号的数字AppID
TENCENT_TTS_SECRET_ID=腾讯云API密钥SecretId
TENCENT_TTS_SECRET_KEY=腾讯云API密钥SecretKey
TENCENT_TTS_REGION=ap-shanghai
TENCENT_TTS_FEMALE_VOICE_TYPE=101001
TENCENT_TTS_MALE_VOICE_TYPE=101002
TENCENT_TTS_SPEED=-1
TENCENT_TTS_VOLUME=5
TENCENT_TTS_SAMPLE_RATE=16000
```

生产环境建议使用只允许调用语音合成接口的子账号密钥，并定期轮换。禁止把真实密钥写入源码、README、镜像、前端包或 Git。

## 业务流程

1. 客户在“健康提醒”中开启吃饭或睡觉提醒并选择时间。
2. Java 根据客户健康档案中的性别选择相反性别音色：女客户使用温暖男声，男客户使用温柔女声。
3. 系统从经过内容约束的文案模板中随机选择一句，通过腾讯云生成 MP3。
4. MP3 保存到私有 MinIO，生成记录归属当前租户和当前客户。
5. 小程序携带登录态下载并播放，其他用户不能访问该音频。

## 微信后台提醒限制

微信小程序退出或进入后台后不能自行定时唤醒并播放音频。当前版本支持设置保存和即时试听；若要在饭点、睡前准时触达，还需申请微信订阅消息模板，取得模板 ID，并由用户在小程序内授权订阅。通知到达后，用户点击消息进入小程序，再播放动态语音。

## 接口

- `GET /api/v1/me/voice-reminders`：读取本人提醒设置。
- `PUT /api/v1/me/voice-reminders`：保存本人提醒设置。
- `POST /api/v1/me/voice-reminders/preview`：生成一条动态试听语音。
- `GET /api/v1/me/voice-reminders/audio/{id}/content`：下载本人私有试听音频。

所有接口只允许个人客户工作台访问。
