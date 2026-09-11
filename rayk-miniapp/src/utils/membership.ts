export const MEMBERSHIP_BENEFIT_NOT_AVAILABLE = 60401
export const HEALTH_TREE_HOLE_TRIAL_EXPIRED = 60740

type MembershipPromptOptions = {
  title?: string
  content?: string
}

let promptVisible = false

/** Returns true for stable backend errors that require the customer to upgrade. */
export function isMembershipBenefitExhausted(cause: unknown): boolean {
  if (!cause || typeof cause !== 'object') return false
  const code = Number((cause as { code?: unknown }).code)
  return code === MEMBERSHIP_BENEFIT_NOT_AVAILABLE || code === HEALTH_TREE_HOLE_TRIAL_EXPIRED
}

/** Matches the stable message persisted on an asynchronously failed report. */
export function isMembershipBenefitMessage(message: unknown): boolean {
  return typeof message === 'string' && /会员权益不足|权益已过期/.test(message)
}

/**
 * Show one consistent upgrade prompt for every customer entitlement entry point.
 * The backend remains the source of truth; this only presents the 60401 response.
 */
export function showMembershipUpgradePrompt(options: MembershipPromptOptions = {}) {
  if (promptVisible) return
  promptVisible = true
  uni.showModal({
    title: options.title || '会员权益次数已用完',
    content:
      options.content ||
      '免费客户的部分健康服务有使用次数限制，开通年度健康会员后可继续使用。',
    confirmText: '开通会员',
    cancelText: '暂不',
    success: (result) => {
      if (result.confirm) uni.navigateTo({ url: '/pages-customer/member/subscribe' })
    },
    complete: () => {
      promptVisible = false
    },
  })
}

/** Handle a quota error and report whether the caller should stop its generic error UI. */
export function handleMembershipBenefitError(
  cause: unknown,
  options: MembershipPromptOptions = {},
): boolean {
  if (!isMembershipBenefitExhausted(cause)) return false
  showMembershipUpgradePrompt(options)
  return true
}
