# 三羊健康会员 SVG 资源

说明：
- 本文件夹已按页面分为 `free-member`、`yearly-member`、`open-member`、`common` 四个目录。
- 当前文件仍使用 **SVG 格式文件**，复杂插画保留为 SVG 内嵌位图，以保证与已确认的视觉稿一致；位图已经按小程序实际显示尺寸缩放和压缩。
- `common` 中保留跨会员页面复用的图标，页面不再携带重复副本，减少微信代码包体积。
- `replacements` 中的会员标识和小组件为轻量纯 SVG 路径资源。
- 如果后续需要“纯矢量路径版 SVG（非嵌入位图）”，需要重新绘制复杂插画，不能只修改文件扩展名。

微信小程序引用路径示例：
- `/pages-customer/static/member/free-member/member-free-hero-shield.svg`
- `/pages-customer/static/member/common/member-ai-assessment.svg`
- `/pages-customer/static/member/yearly-member/member-yearly-health-trend.svg`
