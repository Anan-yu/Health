const HEALTH_TEXT_TRANSLATIONS: Array<[string, string]> = [
  ['VERY_HIGH', '很高'],
  ['VERY_POOR', '很差'],
  ['VERY_GOOD', '很好'],
  ['EXCELLENT', '优秀'],
  ['SOMETIMES', '有时'],
  ['3_5_PER_WEEK', '每周 3–5 次'],
  ['1_2_PER_WEEK', '每周 1–2 次'],
  ['OCCASIONAL', '偶尔'],
  ['REGULAR', '经常'],
  ['DAILY', '几乎每天'],
  ['RARELY', '很少'],
  ['ALWAYS', '总是'],
  ['NEVER', '从不'],
  ['MEDIUM', '中等'],
  ['NORMAL', '正常'],
  ['ABNORMAL', '异常'],
  ['CURRENT', '当前'],
  ['FORMER', '既往'],
  ['POOR', '较差'],
  ['FAIR', '一般'],
  ['GOOD', '良好'],
  ['HIGH', '较高'],
  ['LOW', '较低'],
  ['NONE', '无'],
  ['YES', '有'],
  ['NO', '无'],
  ['BMI', '体质指数'],
]

export function cleanHealthText(value: string) {
  const withoutInternalMetrics = value
    .replace(/\s*[（(][A-Za-z][A-Za-z0-9_]*\s*=\s*[^）)]*[）)]/g, '')
    .replace(
      /\b[A-Za-z][A-Za-z0-9_]*\s*=\s*[-+]?\d+(?:\.\d+)?(?:\s*[A-Za-z/%^0-9]+)?\b/g,
      '',
    )
  return HEALTH_TEXT_TRANSLATIONS.reduce(
    (translated, [source, target]) => translated.split(source).join(target),
    withoutInternalMetrics,
  )
    .replace(/\s+/g, ' ')
    .replace(/\s+([，。；：])/g, '$1')
    .trim()
}
