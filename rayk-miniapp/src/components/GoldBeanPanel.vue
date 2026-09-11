<template>
  <view class="page gold-bean-page elder-page">
    <view v-if="!goldBeanEnabled" class="disabled-card">
      <view class="disabled-title">金豆会员暂未开放</view>
      <view class="disabled-copy">当前环境暂未开放金豆会员功能，请联系管理员。</view>
    </view>

    <PageState v-else :loading="loading" :error="error" :empty="!summary">
      <view v-if="summary" class="content">
        <view class="hero-card">
          <view class="hero-topline">
            <view>
              <view class="eyebrow">GOLD BEAN MEMBERSHIP</view>
              <view class="hero-title">{{ summary.memberLevelName }}</view>
            </view>
            <view class="level-badge">{{ summary.historicalLevelName }} · 历史最高</view>
          </view>
          <view class="hero-caption">
            {{ summary.registrationFeeStatus === 'PAID' ? '已完成平台注册并开始累计金豆' : '完成平台支付后开始累计金豆' }}
          </view>
          <view class="hero-stats">
            <view class="hero-stat">
              <text class="hero-stat-value">{{ formatGoldBeanBalance(summary.totalBalance) }}</text>
              <text class="hero-stat-label">金豆总额</text>
            </view>
            <view class="hero-stat">
              <text class="hero-stat-value">{{ summary.directReferralCount }}</text>
              <text class="hero-stat-label">直推人数</text>
            </view>
            <view class="hero-stat">
              <text class="hero-stat-value">{{ summary.tradeLimitPercent }}%</text>
              <text class="hero-stat-label">交易额度</text>
            </view>
          </view>
        </view>

        <view v-if="summary.reminderText" class="reminder-card">
          <view class="reminder-title">当前提醒</view>
          <view class="reminder-copy">{{ summary.reminderText }}</view>
        </view>

        <view class="section-label"><view class="section-line" />金豆账户</view>
        <view class="balance-grid">
          <view class="balance-card bank-card">
            <view class="balance-label">数字银行</view>
            <view class="balance-value">{{ formatGoldBeanBalance(summary.digitalBankBalance) }}</view>
            <view class="balance-note">平台购豆与每日奖励各半进入</view>
          </view>
          <view class="balance-card trading-card">
            <view class="balance-label">可交易金豆</view>
            <view class="balance-value">{{ formatGoldBeanBalance(summary.tradingBalance) }}</view>
            <view class="balance-note">平台购豆与每日奖励各半进入</view>
          </view>
        </view>

        <view v-if="summary.registrationFeeStatus === 'PAID'" class="card robot-card">
          <view class="robot-card-heading">
            <view>
              <view class="card-heading">机器人权益</view>
            </view>
            <view v-if="robotStatus?.redeemedCount" class="robot-count">已兑换 {{ robotStatus.redeemedCount }} 次</view>
          </view>
          <view class="robot-copy">每位用户仅可使用数字银行金豆兑换一次，消耗10000金豆，兑换后扫码加入微信群，由专人对接机器人的权益使用。</view>
          <view v-if="robotStatusLoading" class="field-helper">正在检查兑换资格…</view>
          <view v-else-if="robotStatusError" class="field-helper robot-error">兑换信息暂时不可用，请刷新页面重试。</view>
          <view v-else-if="robotStatus && robotStatus.redeemedCount > 0" class="field-helper robot-done">机器人权益已兑换，每位用户仅限一次。</view>
          <button class="primary-button robot-button" :disabled="robotRedeeming || robotStatusLoading || !robotStatus?.canRedeem" @click="redeemRobot">{{ robotRedeeming ? '兑换中…' : robotStatus?.redeemedCount ? '已兑换' : '兑换机器人权益' }}</button>
        </view>

        <view v-if="summary.registrationFeeStatus === 'PAID'" class="card network-card">
          <view
            class="network-header"
            role="button"
            :aria-expanded="networkExpanded"
            :aria-label="networkExpanded ? '收起推荐关系图' : '展开推荐关系图'"
            @tap="toggleNetwork"
          >
            <view class="network-title-wrap">
              <view class="card-heading">推荐关系图</view>
              <view class="network-summary">直推 {{ referralNetwork?.directReferralCount ?? summary.directReferralCount }} 人 · 两级团队 {{ referralNetwork?.teamMemberCount ?? summary.directReferralCount }} 人</view>
            </view>
            <view class="network-toggle"><text>{{ networkExpanded ? '收起' : '展开' }}</text><text class="network-chevron" :class="{ expanded: networkExpanded }">⌄</text></view>
          </view>
          <view v-if="networkExpanded" class="network-content">
            <view v-if="referralNetworkLoading" class="network-state">正在整理推荐关系…</view>
            <view v-else-if="referralNetworkError" class="network-state network-error" role="button" aria-label="重新加载推荐关系" @tap="loadReferralNetwork">关系图暂时加载失败，点击重试</view>
            <template v-else-if="referralNetwork">
              <view class="network-legend">
                <view><text class="legend-dot direct" />直推成员（第1层）</view>
                <view><text class="legend-dot team" />团队成员（第2层）</view>
              </view>
              <view class="network-self-node">
                <view class="network-avatar self">{{ initialOf(referralNetwork.selfDisplayName) }}</view>
                <view class="network-node-name">{{ referralNetwork.selfDisplayName }}</view>
                <view class="network-node-level">{{ referralNetwork.selfLevelName }}</view>
              </view>
              <view v-if="!referralNetwork.branches.length" class="network-empty">还没有推荐成员，分享推荐码后关系会自动显示在这里。</view>
              <scroll-view v-else class="network-scroll" scroll-x :show-scrollbar="false">
                <view class="network-branches" :style="{ width: `${Math.max(referralNetwork.branches.length * 224, 640)}rpx` }">
                  <view v-for="(branch, branchIndex) in referralNetwork.branches" :key="`${branch.displayName}-${branchIndex}`" class="network-branch">
                    <view class="network-stem" />
                    <view class="network-member-node direct-node">
                      <view class="network-avatar direct">{{ initialOf(branch.displayName) }}</view>
                      <view class="network-node-name">{{ branch.displayName }}</view>
                      <view class="network-node-level">{{ branch.memberLevelName }}</view>
                    </view>
                    <view v-if="branch.members.length" class="network-child-stem" />
                    <view v-if="branch.members.length" class="network-child-list">
                      <view v-for="(member, memberIndex) in branch.members" :key="`${member.displayName}-${memberIndex}`" class="network-child-node">
                        <view class="network-avatar team">{{ initialOf(member.displayName) }}</view>
                        <view class="network-child-name">{{ member.displayName }}</view>
                      </view>
                    </view>
                    <view v-if="branch.hiddenMemberCount" class="network-hidden">另有 {{ branch.hiddenMemberCount }} 人</view>
                  </view>
                </view>
              </scroll-view>
              <view v-if="referralNetwork.hiddenDirectCount" class="network-hidden-direct">还有 {{ referralNetwork.hiddenDirectCount }} 位直推成员未在图中展开</view>
              <view v-if="referralNetwork.branches.length > 2" class="network-scroll-tip">可左右滑动查看完整关系</view>
            </template>
          </view>
        </view>

        <view v-if="summary.registrationFeeStatus === 'PAID'" class="card trend-card">
          <view class="trend-header">
            <view>
              <view class="card-heading">直推成长趋势</view>
              <view class="trend-period">最近 7 天 · 累计直推人数</view>
            </view>
            <view class="trend-current"><text>{{ summary.directReferralCount }}</text> 人</view>
          </view>
          <view v-if="referralNetworkLoading" class="trend-state">正在生成趋势…</view>
          <view v-else-if="!trendChartPoints.length" class="trend-state">趋势数据暂不可用</view>
          <template v-else>
            <view class="trend-legend"><text class="trend-legend-line" />直推人数</view>
            <view class="trend-chart" role="img" :aria-label="trendAriaLabel">
              <view class="trend-grid-line top"><text>{{ trendMaxValue }}</text></view>
              <view class="trend-grid-line middle"><text>{{ Math.round(trendMaxValue / 2) }}</text></view>
              <view class="trend-grid-line bottom"><text>0</text></view>
              <view v-for="segment in trendChartSegments" :key="segment.key" class="trend-segment" :style="segment.style" />
              <view v-for="point in trendChartPoints" :key="point.date" class="trend-point" :style="trendPointStyle(point)">
                <view class="trend-point-dot" />
              </view>
            </view>
            <view class="trend-date-row">
              <text v-for="point in trendChartPoints" :key="point.date">{{ point.label }}</text>
            </view>
            <view class="trend-summary">
              <text>当前 {{ summary.memberLevelName }}</text>
              <text v-if="summary.nextLevelName">距{{ summary.nextLevelName }}还差 {{ Math.max(summary.nextLevelThreshold - summary.directReferralCount, 0) }} 人</text>
              <text v-else>已达到最高等级</text>
            </view>
          </template>
          <view class="level-rule">等级门槛：铜牌 2 人、银牌 9 人、金牌 29 人、钻石 50 人；达标后获得对应一次性奖励。</view>
        </view>

        <view class="card reward-card">
          <view class="card-heading">每日奖励与活跃保护</view>
          <view class="reward-row">
            <view class="reward-main"><text class="reward-number">{{ summary.dailyRewardDays }}</text><text> / {{ summary.dailyRewardTotalDays }} 天</text></view>
            <view class="reward-label">普通会员每日 60 金豆</view>
          </view>
          <view class="progress-track reward-track"><view class="progress-fill" :style="{ width: `${dailyProgress}%` }" /></view>
          <view class="protection-copy">{{ protectionText }}</view>
          <view class="protection-rule">每日奖励完成后进入7天活跃保护期；第15-19天未推荐新人会连续提醒，保护期结束后开始掉级，掉级期间保持100%交易额度；掉到普通会员后继续1个自然日未推荐才按50%执行，直推后恢复100%并刷新7天。</view>
        </view>

        <view v-if="summary.registrationFeeStatus !== 'PAID'" class="card register-card">
          <view class="card-heading">{{ summary.paymentEnabled ? '注册金豆会员' : '开发环境注册演示' }}</view>
          <view class="register-copy">{{ registrationCopy }}</view>
          <view class="field-label">注册身份</view>
          <view class="register-mode" role="radiogroup" aria-label="选择注册身份">
            <view class="register-mode-option" :class="{ active: registerMode === 'PLATFORM_ROOT' }" role="radio" :aria-checked="registerMode === 'PLATFORM_ROOT'" @tap="chooseRegistrationMode('PLATFORM_ROOT')">
              <text class="register-mode-title">一级代理</text>
              <text class="register-mode-note">填写平台注册码</text>
            </view>
            <view class="register-mode-option" :class="{ active: registerMode === 'REFERRAL' }" role="radio" :aria-checked="registerMode === 'REFERRAL'" @tap="chooseRegistrationMode('REFERRAL')">
              <text class="register-mode-title">推荐代理</text>
              <text class="register-mode-note">填写推荐人的推荐码</text>
            </view>
          </view>
          <template v-if="registerMode === 'PLATFORM_ROOT'">
            <view class="field-label invite-label">平台注册码（一级代理必填）</view>
            <input v-model="registerForm.platformInviteCode" class="text-input invite-input" maxlength="64" placeholder="请输入平台管理员提供的一次性注册码" />
            <view class="field-helper invite-helper">仅一级代理填写；平台注册码只能使用一次，支付取消或订单过期后会自动释放。</view>
          </template>
          <template v-else>
            <view class="field-label">推荐人的推荐码（推荐代理必填）</view>
            <input v-model="registerForm.referralCode" class="text-input" maxlength="32" placeholder="请输入提供推荐码的推荐人" />
            <view class="field-helper">注册费将结算给该推荐人；推荐人需完成微信收款绑定，不需要填写平台注册码。</view>
          </template>
          <view class="field-label">所在地区（仅支持省和市）</view>
          <picker mode="region" level="city" @change="onRegisterRegionChange">
            <view class="picker-field" :class="{ placeholder: !registerRegionLabel }">
              <text class="picker-text">{{ registerRegionLabel || '请选择所在省和市' }}</text>
              <text class="picker-arrow">›</text>
            </view>
          </picker>
          <view class="field-helper">请选择省和市，系统将按“省 / 市”保存。</view>
          <view v-if="registrationError" class="field-helper register-error" role="alert">{{ registrationError }}</view>
          <button class="primary-button" :loading="registering" :disabled="registering || paymentCapabilityRestricted" @click="register">{{ registrationButtonText }}</button>
        </view>

        <view v-if="summary.paymentEnabled && summary.goldBeanPurchaseEnabled && summary.registrationFeeStatus === 'PAID'" class="card purchase-card">
          <view class="card-heading">向平台购买金豆</view>
          <view class="purchase-copy">每个金豆基础价 ￥{{ (summary.goldBeanUnitPriceCent / 100).toFixed(2) }}，实际支付含 {{ summary.virtualPaymentSurchargePercent }}% 虚拟支付服务费，支付成功后按一半进入可交易金豆、一半进入数字银行。</view>
          <view class="field-label">购买数量</view>
          <view class="quantity-presets">
            <view v-for="quantity in purchasePresets" :key="quantity" class="quantity-preset" :class="{ active: Number(purchaseQuantity) === quantity }" role="button" :aria-pressed="Number(purchaseQuantity) === quantity" @tap="setPurchaseQuantity(quantity)">{{ quantity }}</view>
          </view>
          <input v-model="purchaseQuantity" class="text-input quantity-input" type="number" step="1" inputmode="numeric" placeholder="请输入购买数量" />
          <view class="purchase-total">应付 <text>¥{{ purchaseAmountYuan }}</text></view>
          <button class="primary-button purchase-button" :disabled="purchasing" @click="purchase">{{ purchasing ? '正在打开微信支付…' : `向平台支付 ¥${purchaseAmountYuan}` }}</button>
        </view>

        <view v-if="summary.registrationFeeStatus === 'PAID'" class="card market-entry-card" role="button" aria-label="进入金豆集市" @tap="openMarket">
          <view class="market-entry-icon">市</view>
          <view class="market-entry-copy">
            <view class="market-entry-title">金豆集市</view>
            <view class="market-entry-note">浏览挂单、发布金豆，交易操作集中管理</view>
          </view>
          <view class="market-entry-arrow">›</view>
        </view>

        <view v-if="summary.registrationFeeStatus === 'PAID' && summary.referralCode" class="card referral-card">
          <view class="referral-header">
            <view class="card-heading">我的推荐码</view>
            <view class="referral-copy-hint">长按推荐码即可复制</view>
          </view>
          <text class="referral-code" selectable>{{ summary.referralCode }}</text>
          <view class="referral-note">直推注册奖励 11 金豆；非直推下游注册时，各级上级代理各奖励 5 金豆。</view>
        </view>

        <view v-if="summary.registrationFeeStatus === 'PAID' && referralSettlements.length" class="card settlement-card">
          <view class="settlement-header">
            <view class="card-heading">推荐奖励收款</view>
            <view v-if="referralSettlementLoading" class="settlement-loading">更新中…</view>
          </view>
          <view class="settlement-copy">被推荐人完成注册支付后，已开通自动收款会直接入账；未开通时请在微信内确认收款。</view>
          <view v-for="(item, index) in referralSettlements" :key="item.orderNo" class="settlement-row">
            <view class="settlement-main">
              <view class="settlement-title">注册奖励 ¥{{ (item.amountCent / 100).toFixed(2) }}</view>
              <view class="settlement-meta">{{ referralSettlementStatusText(item) }}<text v-if="item.transferState && transferStateText(item.transferState) !== referralSettlementStatusText(item)"> · {{ transferStateText(item.transferState) }}</text></view>
            </view>
            <view class="settlement-actions">
              <button v-if="item.userConfirmationRequired && item.packageInfo" class="settlement-button" :disabled="referralSettlementActionOrder !== '' || referralAuthorizationAction" @tap="confirmReferralSettlement(item)">{{ referralSettlementActionOrder === item.orderNo ? '处理中…' : '确认收款' }}</button>
              <button v-else-if="item.settlementStatus === 'PENDING'" class="settlement-button secondary-settlement-button" :disabled="referralSettlementActionOrder !== '' || referralAuthorizationAction" @tap="refreshReferralSettlement(item)">{{ referralSettlementActionOrder === item.orderNo ? '查询中…' : '刷新状态' }}</button>
              <view v-else-if="item.settlementStatus === 'SETTLED'" class="settlement-success">已到账</view>
              <button v-if="index === 0 && referralAuthorization && !referralAuthorization.authorized" class="settlement-button auto-receive-inline-button" :disabled="referralSettlementActionOrder !== '' || referralAuthorizationAction" @tap="enableReferralAutoReceive">{{ referralAuthorizationAction ? '处理中…' : referralAuthorization.userConfirmationRequired ? '继续授权' : '自动收款' }}</button>
            </view>
          </view>
        </view>

        <view v-if="summary.registrationFeeStatus === 'PAID' && tradePayouts.length" class="card settlement-card">
          <view class="settlement-header">
            <view class="card-heading">集市卖家收款</view>
            <view v-if="tradePayoutLoading" class="settlement-loading">更新中…</view>
          </view>
          <view class="settlement-copy">买家完成金豆虚拟道具支付后，平台商户向卖家转账；只有转账成功后，买家才会获得金豆。</view>
          <view v-for="item in tradePayouts" :key="item.tradeNo" class="settlement-row">
            <view class="settlement-main">
              <view class="settlement-title">卖出 {{ formatGoldBeanAmount(item.quantity) }} 豆 · ¥{{ (item.amountCent / 100).toFixed(2) }}</view>
              <view class="settlement-meta">{{ tradePayoutStatusText(item) }}<text v-if="item.transferState && transferStateText(item.transferState) !== tradePayoutStatusText(item)"> · {{ transferStateText(item.transferState) }}</text></view>
            </view>
            <view class="settlement-actions">
              <button v-if="item.userConfirmationRequired && item.packageInfo" class="settlement-button" :disabled="tradePayoutActionTradeNo !== ''" @tap="confirmTradePayout(item)">{{ tradePayoutActionTradeNo === item.tradeNo ? '处理中…' : '确认收款' }}</button>
              <button v-else-if="item.settlementStatus === 'PAYMENT_CONFIRMED' || item.settlementStatus === 'TRANSFER_PENDING'" class="settlement-button secondary-settlement-button" :disabled="tradePayoutActionTradeNo !== ''" @tap="refreshTradePayout(item)">{{ tradePayoutActionTradeNo === item.tradeNo ? '查询中…' : '刷新状态' }}</button>
              <view v-else-if="item.settlementStatus === 'COMPLETED'" class="settlement-success">已到账</view>
            </view>
          </view>
        </view>

        <view v-if="summary.regionOpenAllowed || summary.regionCity" class="card region-card">
          <view class="card-heading">区域权限</view>
          <view v-if="summary.regionCity" class="region-current">已开辟：{{ summary.regionCity }}</view>
           <view v-else class="region-copy">钻石会员可申请开辟一个城市区域，区域不可更改，推荐链最多三级。区域内代理推荐注册产生奖励时，以开辟人本人实际获得的注册推荐奖励为基数，仅给直接推荐人额外返利 20%。</view>
           <view v-if="summary.regionOpenAllowed" class="field-label">所在地区（仅支持省和市）</view>
           <picker v-if="summary.regionOpenAllowed" mode="region" level="city" @change="onRegionChange">
             <view class="picker-field" :class="{ placeholder: !regionLabel }">
               <text class="picker-text">{{ regionLabel || '请选择所在省和市' }}</text>
               <text class="picker-arrow">›</text>
             </view>
           </picker>
           <view v-if="summary.regionOpenAllowed" class="field-helper">请选择省和市，系统将按“省 / 市”保存。</view>
           <button v-if="summary.regionOpenAllowed" class="secondary-button" :disabled="openingRegion" @click="openRegion">{{ openingRegion ? '正在申请…' : '申请开辟区域' }}</button>
        </view>

        <view class="card ledger-card">
          <view class="ledger-header">
            <view class="card-heading">最近金豆记录</view>
            <view class="text-button ledger-expand-button" hover-class="text-button-hover" role="button" :aria-expanded="ledgerExpanded" :aria-label="ledgerExpanded ? '收起最近金豆记录' : '展开最近金豆记录'" @tap="toggleLedger"><text class="text-button-label">{{ ledgerExpanded ? '收起' : '展开' }}</text></view>
          </view>
          <view v-if="!ledgerExpanded" class="ledger-collapsed" role="button" aria-label="展开最近金豆记录" @tap="toggleLedger">
            <text v-if="ledgerLoading">正在加载记录…</text>
            <text v-else-if="!ledger.length">暂无金豆记录</text>
            <text v-else>共 {{ ledger.length }} 条记录，点击展开查看明细</text>
          </view>
          <view v-else>
            <view v-if="ledgerLoading" class="ledger-empty">正在加载记录…</view>
            <view v-else-if="!ledger.length" class="ledger-empty">暂无金豆记录</view>
            <view v-else v-for="item in ledger" :key="item.id" class="ledger-row">
              <view class="ledger-copy"><view class="ledger-title">{{ item.description }}</view><view class="ledger-meta">{{ formatDate(item.createdAt) }} · {{ bucketName(item.bucket) }}</view></view>
              <view class="ledger-amount" :class="item.direction === 'DEBIT' ? 'debit' : ''">{{ item.direction === 'DEBIT' ? '-' : '+' }}{{ formatGoldBeanAmount(item.amount) }}</view>
            </view>
          </view>
        </view>

        <view class="disclaimer">金豆服务中的注册、支付、交易与区域返利均以服务端校验和账本记录为准。</view>
      </view>
    </PageState>

    <view v-if="robotDialogVisible" class="robot-dialog-mask" catchtouchmove="true">
      <view class="robot-dialog" role="dialog" aria-modal="true" @tap.stop>
        <view class="robot-dialog-header">
          <view>
            <view class="robot-dialog-kicker">兑换成功</view>
            <view class="robot-dialog-title">加入企业微信群</view>
          </view>
          <view class="robot-dialog-close" role="button" aria-label="关闭入群弹窗" @tap="closeRobotDialog">×</view>
        </view>
        <view class="robot-dialog-pill">已扣除 {{ formatGoldBeanAmount(robotRedemption?.goldBeanCost || 10000) }} 数字银行金豆</view>
        <view class="robot-dialog-copy">请扫码加入 {{ robotRedemption?.groupName || '机器人权益服务群' }}，群内有专人对接你的机器人权益。</view>
        <view v-if="robotRedemption" class="robot-qr-frame">
          <image class="robot-qr-image" :src="robotQrImage" mode="aspectFit" show-menu-by-longpress @error="robotQrError = true" />
          <view v-if="robotQrError" class="robot-qr-error">二维码加载失败，请联系管理员获取最新入群方式。</view>
        </view>
        <view v-else class="robot-qr-error">当前未返回入群二维码，请联系管理员。</view>
        <view class="robot-qr-order-tip">请先长按图片保存二维码保存到手机，再点击扫一扫加入对接群。</view>
        <view class="robot-dialog-actions">
          <view class="secondary-button robot-action-button robot-action-hint">长按图片保存二维码</view>
          <button class="primary-button robot-action-button" :loading="robotQrScanning" :disabled="robotQrScanning || robotQrError || !robotQrImage" @tap="scanRobotQr">{{ robotQrScanning ? '打开中…' : '扫一扫加入' }}</button>
        </view>
        <button class="primary-button robot-dialog-button" @tap="closeRobotDialog">我已知道</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import PageState from '@/components/PageState.vue'
import { goldBeanEnabled } from '@/constants/features'
import { formatGoldBeanAmount, formatGoldBeanBalance, isValidIntegerGoldBeanAmount } from '@/utils/gold-bean-amount'
import robotGroupQrAsset from '@/assets/ui/gold-bean/robot-group-qr.jpg'
import {
  createGoldBeanPurchaseOrder,
  createGoldBeanRegistrationOrder,
  createWechatGoldBeanPayment,
  cancelGoldBeanOrder,
  beginGoldBeanReferralAuthorization,
  getGoldBeanReferralAuthorization,
  getGoldBeanReferralNetwork,
  getGoldBeanReferralSettlements,
  getGoldBeanTradePayouts,
  syncGoldBeanTradePayout,
  syncGoldBeanReferralAuthorization,
  syncGoldBeanReferralSettlement,
  getGoldBeanLedger,
  getGoldBeanOrder,
  getGoldBeanSummary,
  getGoldRobotStatus,
  openGoldRegion,
  redeemGoldRobot,
  registerGoldBean,
  type GoldBeanLedgerEntry,
  type GoldBeanPaymentParams,
  type GoldBeanReferralAuthorization,
  type GoldBeanReferralNetwork,
  type GoldBeanReferralSettlement,
  type GoldBeanTradePayout,
  type GoldBeanSummary,
  type GoldRobotRedemption,
  type GoldRobotStatus,
} from '@/api/gold-bean'

const loading = ref(true)
const error = ref('')
const summary = ref<GoldBeanSummary>()
const ledger = ref<GoldBeanLedgerEntry[]>([])
const ledgerLoading = ref(false)
const ledgerExpanded = ref(false)
const registering = ref(false)
const registrationError = ref('')
const paymentCapabilityRestricted = ref(false)
const purchasing = ref(false)
const openingRegion = ref(false)
const registerMode = ref<'PLATFORM_ROOT' | 'REFERRAL'>('PLATFORM_ROOT')
const registerForm = reactive({ referralCode: '', platformInviteCode: '', province: '', city: '' })
const regionForm = reactive({ province: '', city: '' })
const purchaseQuantity = ref('10')
const robotStatus = ref<GoldRobotStatus>()
const robotStatusLoading = ref(false)
const robotStatusError = ref(false)
const robotRedeeming = ref(false)
const robotDialogVisible = ref(false)
const robotRedemption = ref<GoldRobotRedemption>()
const robotQrError = ref(false)
const robotQrScanning = ref(false)
const robotRequestId = ref('')
const referralSettlements = ref<GoldBeanReferralSettlement[]>([])
const referralSettlementLoading = ref(false)
const referralSettlementActionOrder = ref('')
const referralAuthorization = ref<GoldBeanReferralAuthorization>()
const referralAuthorizationLoading = ref(false)
const referralAuthorizationAction = ref(false)
const tradePayouts = ref<GoldBeanTradePayout[]>([])
const tradePayoutLoading = ref(false)
const tradePayoutActionTradeNo = ref('')
const referralNetwork = ref<GoldBeanReferralNetwork>()
const referralNetworkLoading = ref(false)
const referralNetworkError = ref(false)
const networkExpanded = ref(false)
const trendChartSize = ref({ width: 0, height: 0 })
const trendComponentInstance = getCurrentInstance()
const BUNDLED_GROUP_QR_IMAGE_URL = 'asset://robot-group-qr'
const robotQrImage = computed(() => {
  const imageUrl = robotRedemption.value?.groupQrImageUrl
  return !imageUrl || imageUrl === BUNDLED_GROUP_QR_IMAGE_URL ? robotGroupQrAsset : imageUrl
})

type RegionPickerChangeEvent = { detail?: { value?: unknown } }

const registerRegionLabel = computed(() => {
  if (!registerForm.province || !registerForm.city) return ''
  return `${registerForm.province} / ${registerForm.city}`
})
const regionLabel = computed(() => {
  if (!regionForm.province || !regionForm.city) return ''
  return `${regionForm.province} / ${regionForm.city}`
})

const TREND_CHART_LEFT = 4
const TREND_CHART_RIGHT = 4
const TREND_CHART_TOP = 12
const TREND_CHART_BOTTOM = 8
const TREND_CHART_PLOT_WIDTH = 100 - TREND_CHART_LEFT - TREND_CHART_RIGHT
const TREND_CHART_PLOT_HEIGHT = 100 - TREND_CHART_TOP - TREND_CHART_BOTTOM
const trendMaxValue = computed(() => Math.max(
  1,
  ...(referralNetwork.value?.trend || []).map((point) => point.directReferralCount),
))
const trendChartPoints = computed(() => {
  const trend = referralNetwork.value?.trend || []
  const lastIndex = Math.max(trend.length - 1, 1)
  const chartWidth = trendChartSize.value.width
  const chartHeight = trendChartSize.value.height
  const hasMeasuredGeometry = chartWidth > 0 && chartHeight > 0
  return trend.map((point, index) => ({
    date: point.date,
    label: point.date.slice(5).replace('-', '/'),
    value: point.directReferralCount,
    levelName: point.levelName,
    x: TREND_CHART_LEFT + (index / lastIndex) * TREND_CHART_PLOT_WIDTH,
    y: TREND_CHART_BOTTOM + Math.min(1, point.directReferralCount / trendMaxValue.value) * TREND_CHART_PLOT_HEIGHT,
    xPx: hasMeasuredGeometry
      ? (chartWidth * (TREND_CHART_LEFT + (index / lastIndex) * TREND_CHART_PLOT_WIDTH)) / 100
      : null,
    yPx: hasMeasuredGeometry
      ? (chartHeight * (TREND_CHART_BOTTOM + Math.min(1, point.directReferralCount / trendMaxValue.value) * TREND_CHART_PLOT_HEIGHT)) / 100
      : null,
  }))
})
const trendChartSegments = computed(() => {
  const points = trendChartPoints.value
  const segments: Array<{ key: string; style: Record<string, string> }> = []
  let startIndex = 0
  while (startIndex < points.length - 1) {
    let endIndex = startIndex + 1
    while (
      endIndex < points.length - 1
      && points[endIndex].value === points[startIndex].value
      && points[endIndex + 1].value === points[startIndex].value
    ) {
      endIndex += 1
    }

    const point = points[startIndex]
    const next = points[endIndex]
    if (point.xPx !== null && point.yPx !== null && next.xPx !== null && next.yPx !== null) {
      const deltaX = next.xPx - point.xPx
      const deltaY = next.yPx - point.yPx
      const width = Math.sqrt(deltaX * deltaX + deltaY * deltaY)
      const renderedWidth = Math.max(width - Math.min(1.5, width / 2), 0)
      const angle = -Math.atan2(deltaY, deltaX) * (180 / Math.PI)
      segments.push({
        key: `${point.date}-${next.date}`,
        style: {
          left: `${point.xPx}px`,
  bottom: `${Math.max(point.yPx - 1, 0)}px`,
          width: `${renderedWidth}px`,
          transform: `rotate(${angle}deg)`,
        },
      })
    }
    startIndex = endIndex
  }
  return segments
})
const trendPointStyle = (point: {
  x: number
  y: number
  xPx: number | null
  yPx: number | null
}) => ({
  left: point.xPx === null ? `${point.x}%` : `${point.xPx}px`,
  bottom: point.yPx === null ? `${point.y}%` : `${point.yPx}px`,
})
const trendAriaLabel = computed(() => {
  const points = trendChartPoints.value
  if (!points.length) return '最近七天直推成长趋势暂无数据'
  return `最近七天累计直推人数从 ${points[0].value} 人变化到 ${points[points.length - 1].value} 人`
})
let trendMeasureTimer: ReturnType<typeof setTimeout> | undefined
let trendMeasureRetryCount = 0
const measureTrendChart = () => {
  nextTick(() => {
    const query = uni.createSelectorQuery()
    if (trendComponentInstance?.proxy && typeof query.in === 'function') {
      query.in(trendComponentInstance.proxy)
    }
    query
      .select('.trend-chart')
      .boundingClientRect((rect) => {
        const info = rect as { width?: unknown; height?: unknown } | null
        const width = Number(info?.width || 0)
        const height = Number(info?.height || 0)
        if (width <= 0 || height <= 0) {
          if (trendMeasureRetryCount < 8 && !trendMeasureTimer) {
            trendMeasureRetryCount += 1
            trendMeasureTimer = setTimeout(() => {
              trendMeasureTimer = undefined
              measureTrendChart()
            }, 60)
          }
          return
        }
        trendMeasureRetryCount = 0
        if (width === trendChartSize.value.width && height === trendChartSize.value.height) return
        trendChartSize.value = { width, height }
      })
      .exec()
  })
}
watch(
  [
    () => trendChartPoints.value.map((point) => `${point.date}:${point.value}`).join('|'),
    () => trendChartSize.value.width,
    () => trendChartSize.value.height,
    () => referralNetworkLoading.value,
  ],
  () => {
    if (!referralNetworkLoading.value) {
      measureTrendChart()
    }
  },
  { flush: 'post' },
)
let trendResizeHandler: (() => void) | undefined
onMounted(() => {
  measureTrendChart()
  if (typeof globalThis !== 'undefined' && typeof globalThis.addEventListener === 'function') {
    trendResizeHandler = measureTrendChart
    globalThis.addEventListener('resize', trendResizeHandler)
  }
})
onBeforeUnmount(() => {
  if (trendMeasureTimer) {
    globalThis.clearTimeout(trendMeasureTimer)
    trendMeasureTimer = undefined
  }
  if (typeof globalThis !== 'undefined' && trendResizeHandler) {
    globalThis.removeEventListener('resize', trendResizeHandler)
  }
})
const dailyProgress = computed(() => {
  const current = summary.value?.dailyRewardDays || 0
  const total = summary.value?.dailyRewardTotalDays || 20
  return Math.min(100, Math.round((current / Math.max(total, 1)) * 100))
})
const protectionText = computed(() => {
  const current = summary.value
  const dailyDays = current?.dailyRewardDays || 0
  const dailyTotal = current?.dailyRewardTotalDays || 20
  if (dailyDays < dailyTotal && !current?.protectionUntil) {
    return `每日奖励进行中，完成${dailyTotal}天奖励后进入7天活跃保护期；第15-19天未推荐新人会连续提醒。`
  }
  if (!current?.protectionUntil) {
    if (current?.memberLevel === 'ORDINARY' && current.tradeLimitPercent < 100) {
      return `当前为普通会员，活跃保护期已结束，交易额度按 ${current.tradeLimitPercent}% 执行。`
    }
    return '当前没有活跃保护期；掉级期间保持100%交易额度，掉到普通会员后继续1个自然日未推荐才限制50%。完成直推可恢复1个等级并刷新7天保护期。'
  }
  const deadline = Date.parse(current.protectionUntil.replace(' ', 'T'))
  if (!Number.isNaN(deadline) && deadline <= Date.now()) {
    return current.memberLevel === 'ORDINARY'
      ? current.tradeLimitPercent < 100
        ? `保护期已结束，当前为普通会员，交易额度按 ${current.tradeLimitPercent}% 执行。`
        : '保护期已结束，当前仍保持100%交易额度；普通会员继续1个自然日未推荐后限制50%。'
      : '保护期已结束，掉级期间保持100%交易额度；未推荐新人时等级每天降低一级，掉到普通会员后次日限制50%。'
  }
  return `当前${current.memberLevelName}活跃保护期至 ${formatDate(current.protectionUntil)}；第4-6天连续提醒，7天内成功推荐新人可刷新7天。`
})
const purchasePresets = [10, 100, 1000]
const surchargedPaymentAmountCent = (baseAmountCent: number) => {
  const percent = summary.value?.virtualPaymentSurchargePercent ?? 12
  return Math.ceil((baseAmountCent * (100 + percent)) / 100)
}
const purchaseAmountYuan = computed(() => {
  const unitPrice = summary.value?.goldBeanUnitPriceCent || 100
  const quantity = Math.max(0, Number(purchaseQuantity.value) || 0)
  const baseAmountCent = Math.ceil(quantity * unitPrice - Number.EPSILON)
  return (surchargedPaymentAmountCent(baseAmountCent) / 100).toFixed(2)
})
const registrationCopy = computed(() => {
  const currentSummary = summary.value
  if (!currentSummary?.paymentEnabled) {
    return '开发环境仅记录注册关系与收款方，不会扣款；一级代理需填写平台注册码，推荐代理需填写推荐人的推荐码。'
  }
  const feeCent = registerMode.value === 'PLATFORM_ROOT'
    ? (currentSummary.platformRegistrationFeeCent ?? currentSummary.registrationFeeCent)
    : (currentSummary.referralRegistrationFeeCent ?? currentSummary.registrationFeeCent)
  const surchargePercent = currentSummary.virtualPaymentSurchargePercent ?? 12
  const paymentFeeCent = surchargedPaymentAmountCent(feeCent)
  return registerMode.value === 'PLATFORM_ROOT'
    ? `一级代理填写平台提供的一次性注册码，基础注册费 ¥${(feeCent / 100).toFixed(2)}，实际支付 ¥${(paymentFeeCent / 100).toFixed(2)}（含${surchargePercent}%虚拟支付服务费）。`
    : `推荐代理填写推荐人的推荐码，基础注册费 ¥${(feeCent / 100).toFixed(2)}，实际支付 ¥${(paymentFeeCent / 100).toFixed(2)}（含${surchargePercent}%虚拟支付服务费），平台仍按基础金额结算给推荐人。`
})
const registrationButtonText = computed(() => {
  if (paymentCapabilityRestricted.value) return '请先处理微信支付限制'
  const feeCent = registerMode.value === 'PLATFORM_ROOT'
    ? (summary.value?.platformRegistrationFeeCent ?? summary.value?.registrationFeeCent ?? 0)
    : (summary.value?.referralRegistrationFeeCent ?? summary.value?.registrationFeeCent ?? 0)
  const paymentFeeCent = surchargedPaymentAmountCent(feeCent)
  if (registering.value) return summary.value?.paymentEnabled ? '正在打开微信支付…' : '正在记录…'
  return summary.value?.paymentEnabled
    ? `向平台支付 ¥${(paymentFeeCent / 100).toFixed(2)}`
    : '记录平台收款注册状态'
})

const formatDate = (value?: string) => (value ? value.replace('T', ' ').slice(0, 16) : '—')
const bucketName = (value: string) => (value === 'DIGITAL_BANK' ? '数字银行金豆' : '可交易金豆')
const transferStateText = (value?: string | null) => {
  const states: Record<string, string> = {
    ACCEPTED: '已受理',
    PROCESSING: '处理中',
    WAIT_USER_CONFIRM: '等待确认收款',
    TRANSFERING: '转账中',
    SUCCESS: '转账成功',
    FAIL: '转账失败',
    CANCELING: '撤销中',
    CANCELLED: '已撤销',
  }
  return value ? (states[value] || value) : '待发起'
}
const referralSettlementStatusText = (item: GoldBeanReferralSettlement) => {
  if (item.settlementStatus === 'SETTLED') return '奖励已到账'
  if (item.settlementStatus === 'FAILED') return '收款失败，请联系平台'
  if (item.userConfirmationRequired) return '等待确认收款'
  return '平台处理中'
}
const tradePayoutStatusText = (item: GoldBeanTradePayout) => {
  if (item.settlementStatus === 'COMPLETED') return '卖家款已到账，金豆已交割'
  if (item.settlementStatus === 'REFUND_REQUIRED') return '转账失败，请联系平台处理'
  if (item.userConfirmationRequired) return '等待确认收款'
  return item.transferState ? transferStateText(item.transferState) : '平台处理中'
}

function onRegisterRegionChange(event: RegionPickerChangeEvent) {
  const values = Array.isArray(event.detail?.value)
    ? event.detail.value.map((value) => String(value || '').trim())
    : []
  const [province = '', city = ''] = values
  if (!province || !city) {
    registerForm.province = ''
    registerForm.city = ''
    uni.showToast({ title: '请选择省和市', icon: 'none' })
    return
  }
  registerForm.province = province
  registerForm.city = city
}

function onRegionChange(event: RegionPickerChangeEvent) {
  const values = Array.isArray(event.detail?.value)
    ? event.detail.value.map((value) => String(value || '').trim())
    : []
  const [province = '', city = ''] = values
  if (!province || !city) {
    regionForm.province = ''
    regionForm.city = ''
    uni.showToast({ title: '请选择省和市', icon: 'none' })
    return
  }
  regionForm.province = province
  regionForm.city = city
}

function chooseRegistrationMode(mode: 'PLATFORM_ROOT' | 'REFERRAL') {
  registerMode.value = mode
  registrationError.value = ''
  paymentCapabilityRestricted.value = false
  if (mode === 'PLATFORM_ROOT') registerForm.referralCode = ''
  else registerForm.platformInviteCode = ''
}

function toggleLedger() {
  ledgerExpanded.value = !ledgerExpanded.value
}

function toggleNetwork() {
  networkExpanded.value = !networkExpanded.value
}

const initialOf = (displayName: string) => displayName.trim().slice(0, 1) || '会'

async function loadReferralNetwork() {
  if (referralNetworkLoading.value || summary.value?.registrationFeeStatus !== 'PAID') return
  referralNetworkLoading.value = true
  referralNetworkError.value = false
  try {
    referralNetwork.value = await getGoldBeanReferralNetwork()
  } catch {
    referralNetworkError.value = true
  } finally {
    referralNetworkLoading.value = false
  }
}

async function loadLedger() {
  if (ledgerLoading.value) return
  ledgerLoading.value = true
  try {
    ledger.value = await getGoldBeanLedger()
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '金豆记录加载失败', icon: 'none' })
  } finally {
    ledgerLoading.value = false
  }
}

async function loadRobotStatus() {
  if (robotStatusLoading.value || summary.value?.registrationFeeStatus !== 'PAID') return
  robotStatusLoading.value = true
  robotStatusError.value = false
  try {
    robotStatus.value = await getGoldRobotStatus()
  } catch {
    robotStatusError.value = true
  } finally {
    robotStatusLoading.value = false
  }
}

async function loadReferralSettlements() {
  if (referralSettlementLoading.value) return
  if (summary.value?.registrationFeeStatus !== 'PAID') {
    referralSettlements.value = []
    return
  }
  referralSettlementLoading.value = true
  try {
    referralSettlements.value = await getGoldBeanReferralSettlements()
  } catch {
    // The reward card is supplementary; a temporary failure must not hide the account page.
    referralSettlements.value = []
  } finally {
    referralSettlementLoading.value = false
  }
}

async function loadTradePayouts() {
  if (tradePayoutLoading.value) return
  if (summary.value?.registrationFeeStatus !== 'PAID') {
    tradePayouts.value = []
    return
  }
  tradePayoutLoading.value = true
  try {
    tradePayouts.value = await getGoldBeanTradePayouts()
  } catch {
    // Seller settlement is supplementary; a temporary failure must not hide the account page.
    tradePayouts.value = []
  } finally {
    tradePayoutLoading.value = false
  }
}

async function loadReferralAuthorization() {
  if (referralAuthorizationLoading.value) return
  if (summary.value?.registrationFeeStatus !== 'PAID') {
    referralAuthorization.value = undefined
    return
  }
  referralAuthorizationLoading.value = true
  try {
    referralAuthorization.value = await getGoldBeanReferralAuthorization()
  } catch {
    // Automatic receive is optional; a temporary error must not hide manual settlements.
    referralAuthorization.value = undefined
  } finally {
    referralAuthorizationLoading.value = false
  }
}

async function load() {
  if (!goldBeanEnabled) {
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const loadedSummary = await getGoldBeanSummary()
    if (!loadedSummary) throw new Error('金豆会员数据为空，请重新登录后重试')
    summary.value = loadedSummary
    await Promise.all([
      loadLedger(),
      loadRobotStatus(),
      loadReferralSettlements(),
      loadReferralAuthorization(),
      loadTradePayouts(),
      loadReferralNetwork(),
    ])
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '金豆会员信息加载失败'
  } finally {
    loading.value = false
  }
}

type WechatMerchantTransferApi = {
  canIUse?: (apiName: string) => boolean
  requestMerchantTransfer?: (options: {
    mchId: string
    appId: string
    package: string
    success?: () => void
    fail?: (error: { errMsg?: string }) => void
  }) => void
}

const invokeMerchantTransferPackage = (
  packageInfo: string,
  merchantId: string,
  appId: string,
  actionLabel: string,
) =>
  new Promise<void>((resolve, reject) => {
    // #ifdef MP-WEIXIN
    const wechat = (globalThis as unknown as { wx?: WechatMerchantTransferApi }).wx
    if (!wechat?.requestMerchantTransfer || !packageInfo || !merchantId || !appId) {
      reject(new Error(`当前微信版本不支持${actionLabel}，请更新微信后重试`))
      return
    }
    try {
      if (wechat.canIUse && !wechat.canIUse('requestMerchantTransfer')) {
        reject(new Error(`当前微信版本不支持${actionLabel}，请更新微信后重试`))
        return
      }
    } catch {
      // Older bases may not expose canIUse consistently; the API call below remains the source of truth.
    }
    wechat.requestMerchantTransfer({
      mchId: merchantId,
      appId,
      package: packageInfo,
      success: () => resolve(),
      fail: (failure) => {
        const message = failure?.errMsg || ''
        if (/requestMerchantTransfer:fail:(?:internal|cancel)/i.test(message) || /cancel/i.test(message)) {
          reject(new Error(actionLabel === '收款确认' ? '尚未确认收款' : '尚未完成自动收款授权'))
          return
        }
        reject(new Error(message || `微信${actionLabel}失败`))
      },
    })
    // #endif
    // #ifndef MP-WEIXIN
    reject(new Error(`请在微信小程序中完成${actionLabel}`))
    // #endif
  })

const invokeMerchantTransfer = (settlement: GoldBeanReferralSettlement) => {
  if (!settlement.packageInfo || !settlement.merchantId || !settlement.appId) {
    return Promise.reject(new Error('收款确认参数尚未准备好，请刷新状态后重试'))
  }
  return invokeMerchantTransferPackage(settlement.packageInfo, settlement.merchantId, settlement.appId, '收款确认')
}

async function enableReferralAutoReceive() {
  if (referralAuthorizationAction.value) return
  referralAuthorizationAction.value = true
  try {
    let authorization = await beginGoldBeanReferralAuthorization()
    referralAuthorization.value = authorization
    if (authorization.userConfirmationRequired && authorization.packageInfo && authorization.merchantId && authorization.appId) {
      await invokeMerchantTransferPackage(
        authorization.packageInfo,
        authorization.merchantId,
        authorization.appId,
        '自动收款授权',
      )
      authorization = await syncGoldBeanReferralAuthorization()
      referralAuthorization.value = authorization
    }
    uni.showToast({
      title: authorization.authorized
        ? '自动收款已开启'
        : authorization.userConfirmationRequired
          ? '请完成微信授权后刷新'
          : authorization.state === 'REQUESTING'
            ? '授权申请处理中，请稍后刷新'
            : authorization.state === 'CLOSED'
              ? '授权已关闭，请重新开启'
              : '自动收款尚未生效，请稍后重试',
      icon: authorization.authorized ? 'success' : 'none',
    })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '自动收款授权失败', icon: 'none' })
  } finally {
    referralAuthorizationAction.value = false
  }
}

async function confirmReferralSettlement(settlement: GoldBeanReferralSettlement) {
  if (referralSettlementActionOrder.value || !settlement.packageInfo) return
  referralSettlementActionOrder.value = settlement.orderNo
  try {
    await invokeMerchantTransfer(settlement)
    const latest = await syncGoldBeanReferralSettlement(settlement.orderNo)
    const index = referralSettlements.value.findIndex((item) => item.orderNo === settlement.orderNo)
    if (index >= 0) referralSettlements.value.splice(index, 1, latest)
    uni.showToast({
      title: latest.settlementStatus === 'SETTLED' ? '推荐奖励已到账' : '已打开收款确认，请完成确认后刷新',
      icon: latest.settlementStatus === 'SETTLED' ? 'success' : 'none',
    })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '微信收款确认失败', icon: 'none' })
  } finally {
    referralSettlementActionOrder.value = ''
  }
}

async function refreshReferralSettlement(settlement: GoldBeanReferralSettlement) {
  if (referralSettlementActionOrder.value) return
  referralSettlementActionOrder.value = settlement.orderNo
  try {
    const latest = await syncGoldBeanReferralSettlement(settlement.orderNo)
    const index = referralSettlements.value.findIndex((item) => item.orderNo === settlement.orderNo)
    if (index >= 0) referralSettlements.value.splice(index, 1, latest)
    uni.showToast({
      title: latest.userConfirmationRequired ? '请点击确认收款' : latest.settlementStatus === 'SETTLED' ? '推荐奖励已到账' : '状态已更新',
      icon: latest.settlementStatus === 'SETTLED' ? 'success' : 'none',
    })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '转账状态查询失败', icon: 'none' })
  } finally {
    referralSettlementActionOrder.value = ''
  }
}

async function confirmTradePayout(payout: GoldBeanTradePayout) {
  if (tradePayoutActionTradeNo.value || !payout.packageInfo || !payout.merchantId || !payout.appId) return
  tradePayoutActionTradeNo.value = payout.tradeNo
  try {
    await invokeMerchantTransferPackage(payout.packageInfo, payout.merchantId, payout.appId, '收款确认')
    const latest = await syncGoldBeanTradePayout(payout.tradeNo)
    const index = tradePayouts.value.findIndex((item) => item.tradeNo === payout.tradeNo)
    if (index >= 0) tradePayouts.value.splice(index, 1, latest)
    uni.showToast({
      title: latest.settlementStatus === 'COMPLETED' ? '收款成功，金豆已交割' : '已确认收款，请刷新状态',
      icon: latest.settlementStatus === 'COMPLETED' ? 'success' : 'none',
    })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '微信收款确认失败', icon: 'none' })
  } finally {
    tradePayoutActionTradeNo.value = ''
  }
}

async function refreshTradePayout(payout: GoldBeanTradePayout) {
  if (tradePayoutActionTradeNo.value) return
  tradePayoutActionTradeNo.value = payout.tradeNo
  try {
    const latest = await syncGoldBeanTradePayout(payout.tradeNo)
    const index = tradePayouts.value.findIndex((item) => item.tradeNo === payout.tradeNo)
    if (index >= 0) tradePayouts.value.splice(index, 1, latest)
    uni.showToast({
      title: latest.userConfirmationRequired ? '请点击确认收款' : latest.settlementStatus === 'COMPLETED' ? '金豆已交割' : '状态已更新',
      icon: latest.settlementStatus === 'COMPLETED' ? 'success' : 'none',
    })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '转账状态查询失败', icon: 'none' })
  } finally {
    tradePayoutActionTradeNo.value = ''
  }
}

defineExpose({ refresh: load })

function newRobotRequestId() {
  return `robot-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
}

const ROBOT_BALANCE_INSUFFICIENT_CODE = 60744

function isRobotBalanceInsufficient(cause: unknown) {
  if (!(cause instanceof Error)) return false
  const code = Number((cause as Error & { code?: unknown }).code)
  return code === ROBOT_BALANCE_INSUFFICIENT_CODE || cause.message.includes('余额不足')
}

function showRobotBalanceInsufficient() {
  uni.showModal({
    title: '暂不能兑换',
    content: '数字银行金豆余额不足，请先积累至10000金豆后再兑换。',
    showCancel: false,
    confirmText: '知道了',
  })
}

function closeRobotDialog() {
  robotDialogVisible.value = false
}

function scanRobotQr() {
  if (robotQrScanning.value || robotQrError.value || !robotQrImage.value) return
  robotQrScanning.value = true
  // #ifdef MP-WEIXIN
  uni.scanCode({
    onlyFromCamera: false,
    scanType: ['qrCode'],
    success: () => {
      uni.showModal({
        title: '二维码已识别',
        content: '请按微信页面提示加入机器人权益对接群。',
        showCancel: false,
        confirmText: '知道了',
      })
    },
    fail: (failure) => {
      if (!/cancel/i.test(failure.errMsg || '')) {
        uni.showToast({ title: '扫一扫未完成，请重试', icon: 'none' })
      }
    },
    complete: () => {
      robotQrScanning.value = false
    },
  })
  // #endif
  // #ifndef MP-WEIXIN
  robotQrScanning.value = false
  uni.showToast({ title: '请在微信小程序中使用扫一扫', icon: 'none' })
  // #endif
}

async function redeemRobot() {
  if (robotRedeeming.value || !robotStatus.value?.canRedeem) return
  if (robotStatus.value.digitalBankBalance < robotStatus.value.costGoldBeans) {
    showRobotBalanceInsufficient()
    return
  }
  const confirmed = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '兑换机器人权益',
      content: '每位用户仅可使用数字银行金豆兑换一次，消耗10000金豆，兑换后扫码加入微信群，由专人对接机器人的权益使用。',
      confirmText: '确认兑换',
      success: (result) => resolve(result.confirm),
      fail: () => resolve(false),
    })
  })
  if (!confirmed) return

  robotRedeeming.value = true
  robotRequestId.value = robotRequestId.value || newRobotRequestId()
  try {
    const redemption = await redeemGoldRobot(robotRequestId.value)
    robotRedemption.value = redemption
    robotQrError.value = false
    robotDialogVisible.value = true
    robotRequestId.value = ''
    if (summary.value) {
      summary.value = {
        ...summary.value,
        totalBalance: Math.max(0, summary.value.totalBalance - redemption.goldBeanCost),
        digitalBankBalance: redemption.digitalBankBalance,
      }
    }
    await Promise.all([loadLedger(), loadRobotStatus()])
  } catch (cause) {
    if (isRobotBalanceInsufficient(cause)) {
      showRobotBalanceInsufficient()
    } else {
      uni.showToast({ title: cause instanceof Error ? cause.message : '机器人权益兑换失败', icon: 'none' })
    }
  } finally {
    robotRedeeming.value = false
  }
}

type WechatVirtualPaymentError = Error & { errCode?: number; errMsg?: string }
const VIRTUAL_PAYMENT_CAPABILITY_RESTRICTED_MESSAGE =
  '微信虚拟支付能力未开通或已被限制，请管理员在小程序后台核对虚拟支付权限；当前不会扣款。'
type WechatVirtualPaymentApi = {
  requestVirtualPayment: (options: {
    mode: 'short_series_goods' | 'short_series_coin' | string
    signData: string
    paySig: string
    signature: string
    success?: () => void
    fail?: (error: { errMsg?: string; errCode?: number }) => void
  }) => void
}

const wait = (milliseconds: number) => new Promise((resolve) => setTimeout(resolve, milliseconds))

const invokeWechatPayment = (params: GoldBeanPaymentParams) =>
  new Promise<void>((resolve, reject) => {
    // #ifdef MP-WEIXIN
    const wechat = (globalThis as unknown as { wx?: WechatVirtualPaymentApi }).wx
    if (!wechat?.requestVirtualPayment) {
      reject(new Error('当前微信基础库不支持虚拟支付，请升级微信后重试'))
      return
    }
    wechat.requestVirtualPayment({
      mode: params.mode,
      signData: params.signData,
      paySig: params.paySig,
      signature: params.signature,
      success: () => resolve(),
      fail: (failure) => {
        const code = failure.errCode
        const rawMessage = failure.errMsg || ''
        const restricted = code === -15005
          || /(banned|no permission|access denied|permission.*(restrict|limit|ban)|限制|封禁|无权限)/i.test(rawMessage)
        const message = code === -2
          ? '支付已取消'
          : restricted
            ? VIRTUAL_PAYMENT_CAPABILITY_RESTRICTED_MESSAGE
            : rawMessage || '微信虚拟支付失败'
        reject(Object.assign(new Error(message), { errCode: code, errMsg: failure.errMsg }) as WechatVirtualPaymentError)
      },
    })
    // #endif
    // #ifndef MP-WEIXIN
    reject(new Error('请在微信小程序中完成支付'))
    // #endif
  })

const waitForGoldBeanOrderPaid = async (orderNo: string) => {
  for (let attempt = 0; attempt < 6; attempt += 1) {
    const order = await getGoldBeanOrder(orderNo)
    if (order.status === 'PAID') return true
    await wait(1500)
  }
  return false
}

const isClosedVirtualPaymentOrder = (cause: unknown) => {
  if (!(cause instanceof Error)) return false
  const paymentError = cause as WechatVirtualPaymentError
  return `${paymentError.message} ${paymentError.errMsg || ''}`.toUpperCase().includes('ORDER_CLOSED')
}

const isVirtualPaymentCapabilityRestricted = (cause: unknown) =>
  cause instanceof Error && cause.message === VIRTUAL_PAYMENT_CAPABILITY_RESTRICTED_MESSAGE

const payGoldBeanOrder = async (orderNo: string, successTitle: string) => {
  const payment = await createWechatGoldBeanPayment(orderNo)
  try {
    await invokeWechatPayment(payment)
  } catch (cause) {
    if (isCancelledVirtualPayment(cause) || isVirtualPaymentCapabilityRestricted(cause)) {
      await cancelGoldBeanOrder(orderNo).catch(() => undefined)
    }
    throw cause
  }
  const paid = await waitForGoldBeanOrderPaid(orderNo)
  await load()
  uni.showToast({ title: paid ? successTitle : '支付成功，平台确认中', icon: paid ? 'success' : 'none' })
}

const payRegistrationOrder = async (orderNo: string, recipient?: string) => {
  const payment = await createWechatGoldBeanPayment(orderNo)
  try {
    await invokeWechatPayment(payment)
  } catch (cause) {
    if (isCancelledVirtualPayment(cause) || isVirtualPaymentCapabilityRestricted(cause)) {
      await cancelGoldBeanOrder(orderNo).catch(() => undefined)
    }
    throw cause
  }
  const paid = await waitForGoldBeanOrderPaid(orderNo)
  const order = await getGoldBeanOrder(orderNo)
  await load()
  if (!paid || order.status !== 'PAID') {
    uni.showToast({ title: '支付成功，平台确认中', icon: 'none' })
    return
  }
  if (order.settlementStatus === 'SETTLED') {
    uni.showToast({ title: '会员已开通，推荐人已收款', icon: 'success' })
  } else if (order.settlementStatus === 'FAILED') {
    uni.showToast({ title: '会员已开通，但推荐人收款失败，请联系平台', icon: 'none' })
  } else if (recipient === 'REFERRER') {
    uni.showToast({ title: '会员已开通，推荐人收款确认中', icon: 'none' })
  } else {
    uni.showToast({ title: '会员已开通', icon: 'success' })
  }
}

const isCancelledVirtualPayment = (cause: unknown) => {
  if (!(cause instanceof Error)) return false
  const paymentError = cause as WechatVirtualPaymentError
  return paymentError.errCode === -2 || `${paymentError.message} ${paymentError.errMsg || ''}`.toUpperCase().includes('CANCEL')
}

async function register() {
  if (registering.value) return
  registrationError.value = ''
  paymentCapabilityRestricted.value = false
  if (registerMode.value === 'PLATFORM_ROOT' && !registerForm.platformInviteCode.trim()) {
    uni.showToast({ title: '请输入平台提供的注册码', icon: 'none' })
    return
  }
  if (registerMode.value === 'REFERRAL' && !registerForm.referralCode.trim()) {
    uni.showToast({ title: '请输入推荐人的推荐码', icon: 'none' })
    return
  }
  if (!registerForm.province || !registerForm.city) {
    uni.showToast({ title: '请选择所在省和市', icon: 'none' })
    return
  }
  registering.value = true
  try {
    const platformRoot = registerMode.value === 'PLATFORM_ROOT'
    const registration = {
      referralCode: platformRoot ? undefined : registerForm.referralCode.trim(),
      platformInviteCode: platformRoot ? registerForm.platformInviteCode.trim() : undefined,
      city: registerRegionLabel.value,
    }
    if (summary.value?.paymentEnabled) {
      let order = await createGoldBeanRegistrationOrder(registration)
      try {
        await payRegistrationOrder(order.orderNo, order.registrationFeeRecipient)
      } catch (cause) {
        if (!isClosedVirtualPaymentOrder(cause)) throw cause
        order = await createGoldBeanRegistrationOrder(registration)
        await payRegistrationOrder(order.orderNo, order.registrationFeeRecipient)
      }
    } else {
      summary.value = await registerGoldBean(registration)
      await loadLedger()
      uni.showToast({ title: '开发注册状态已记录', icon: 'success' })
    }
  } catch (cause) {
    paymentCapabilityRestricted.value = isVirtualPaymentCapabilityRestricted(cause)
    const message = isVirtualPaymentCapabilityRestricted(cause)
      ? VIRTUAL_PAYMENT_CAPABILITY_RESTRICTED_MESSAGE
      : cause instanceof Error && cause.message.includes('推荐人尚未完成微信收款绑定')
      ? cause.message
      : isClosedVirtualPaymentOrder(cause)
      ? '微信支付订单已失效，请重新点击支付'
      : cause instanceof Error
        ? cause.message
        : '注册失败'
    registrationError.value = message
    uni.showToast({ title: message, icon: 'none' })
  } finally {
    registering.value = false
  }
}

function setPurchaseQuantity(quantity: number) {
  purchaseQuantity.value = String(Math.min(quantity, summary.value?.maxPurchaseQuantity || quantity))
}

async function purchase() {
  if (purchasing.value) return
  const quantity = Number(purchaseQuantity.value)
  const maxQuantity = summary.value?.maxPurchaseQuantity || 0
  if (!isValidIntegerGoldBeanAmount(quantity, maxQuantity)) {
    uni.showToast({ title: '请输入整数购买数量', icon: 'none' })
    return
  }
  purchasing.value = true
  try {
    let order = await createGoldBeanPurchaseOrder(quantity)
    try {
      await payGoldBeanOrder(order.orderNo, '金豆已到账')
    } catch (cause) {
      if (!isClosedVirtualPaymentOrder(cause)) throw cause
      order = await createGoldBeanPurchaseOrder(quantity)
      await payGoldBeanOrder(order.orderNo, '金豆已到账')
    }
  } catch (cause) {
    const message = isClosedVirtualPaymentOrder(cause)
      ? '微信支付订单已失效，请重新点击购买'
      : cause instanceof Error
        ? cause.message
        : '购买失败'
    uni.showToast({ title: message, icon: 'none' })
  } finally {
    purchasing.value = false
  }
}

function openMarket() {
  uni.navigateTo({ url: '/pages-customer/gold-bean-market/index' })
}

async function openRegion() {
  if (openingRegion.value || !regionForm.province || !regionForm.city) {
    uni.showToast({ title: '请选择省和市', icon: 'none' })
    return
  }
  openingRegion.value = true
  try {
    const region = await openGoldRegion({
      city: regionLabel.value,
    })
    if (summary.value) {
      summary.value = { ...summary.value, regionId: region.id, regionCity: region.city, regionOpenAllowed: false }
    }
    regionForm.province = ''
    regionForm.city = ''
    uni.showToast({ title: '区域申请已记录', icon: 'success' })
  } catch (cause) {
    uni.showToast({ title: cause instanceof Error ? cause.message : '区域申请失败', icon: 'none' })
  } finally {
    openingRegion.value = false
  }
}

</script>

<style scoped>
.gold-bean-page { min-height: 100vh; padding: 24rpx; background: linear-gradient(180deg, #f3fbf7 0%, #f8fbfa 100%); }
.market-entry-card { display: flex; align-items: center; gap: 18rpx; border-color: #bfe5d8; background: linear-gradient(135deg, #f0fbf6, #fff); }
.market-entry-card:active { opacity: .78; }
.market-entry-icon { display: flex; flex: 0 0 64rpx; align-items: center; justify-content: center; width: 64rpx; height: 64rpx; border-radius: 20rpx; color: #0c8066; background: #dff6ed; font-size: 30rpx; font-weight: 800; }
.market-entry-copy { flex: 1; min-width: 0; }
.market-entry-title { color: #173e34; font-size: 29rpx; font-weight: 800; }
.market-entry-note { margin-top: 7rpx; overflow: hidden; color: #7e958d; font-size: 21rpx; text-overflow: ellipsis; white-space: nowrap; }
.market-entry-arrow { flex: none; color: #0c8568; font-size: 42rpx; line-height: 1; }
.content { padding-bottom: 24rpx; }
.hero-card { padding: 34rpx 30rpx 28rpx; border-radius: 32rpx; color: #fff; background: linear-gradient(135deg, #143f34 0%, #0c8066 72%, #18aa86 100%); box-shadow: 0 18rpx 42rpx rgba(8, 91, 70, .18); }
.hero-topline { display: flex; align-items: flex-start; justify-content: space-between; gap: 14rpx; }
.eyebrow { color: rgba(255,255,255,.66); font-size: 18rpx; font-weight: 750; letter-spacing: 3rpx; }
.hero-title { margin-top: 14rpx; font-size: 46rpx; font-weight: 820; line-height: 1.2; }
.level-badge { flex: none; max-width: 240rpx; padding: 10rpx 14rpx; border: 1rpx solid rgba(255,255,255,.2); border-radius: 20rpx; color: #d8f5e9; background: rgba(255,255,255,.1); font-size: 20rpx; line-height: 1.35; text-align: center; }
.hero-caption { margin-top: 12rpx; color: rgba(255,255,255,.78); font-size: 23rpx; }
.hero-stats { display: flex; margin-top: 30rpx; padding-top: 24rpx; border-top: 1rpx solid rgba(255,255,255,.16); }
.hero-stat { flex: 1; text-align: center; }
.hero-stat + .hero-stat { border-left: 1rpx solid rgba(255,255,255,.16); }
.hero-stat-value { display: block; font-size: 40rpx; font-weight: 800; line-height: 1.2; }
.hero-stat-label { display: block; margin-top: 5rpx; color: rgba(255,255,255,.68); font-size: 21rpx; }
.reminder-card { margin-top: 20rpx; padding: 24rpx 26rpx; border: 2rpx solid #f2dfac; border-radius: 26rpx; background: #fff9e9; }
.reminder-title { color: #80580d; font-size: 27rpx; font-weight: 780; }
.reminder-copy { margin-top: 8rpx; color: #8b7351; font-size: 24rpx; line-height: 1.55; }
.section-label { display: flex; align-items: center; gap: 12rpx; margin: 30rpx 8rpx 18rpx; color: #183c34; font-size: 32rpx; font-weight: 800; }
.section-line { width: 8rpx; height: 34rpx; border-radius: 6rpx; background: #23b88e; }
.balance-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16rpx; }
.balance-card { min-height: 194rpx; padding: 24rpx 22rpx; border: 1rpx solid #d6eee5; border-radius: 26rpx; box-sizing: border-box; background: #fff; }
.bank-card { background: linear-gradient(145deg, #f4fffb, #fff); }
.trading-card { border-color: #cce8f1; background: linear-gradient(145deg, #f3fcff, #fff); }
.balance-label { color: #476a60; font-size: 23rpx; font-weight: 700; }
.balance-value { margin-top: 18rpx; color: #0b906e; font-size: 48rpx; font-weight: 820; line-height: 1; }
.trading-card .balance-value { color: #247fa9; }
.balance-note { margin-top: 14rpx; color: #82958f; font-size: 20rpx; line-height: 1.35; }
.card { margin-top: 20rpx; padding: 26rpx; border: 1rpx solid #dcece6; border-radius: 28rpx; background: #fff; box-shadow: 0 10rpx 26rpx rgba(24, 91, 75, .045); }
.card-heading { color: #173e34; font-size: 29rpx; font-weight: 800; }
.progress-track { height: 14rpx; margin-top: 16rpx; overflow: hidden; border-radius: 14rpx; background: #e1f0eb; }
.progress-fill { height: 100%; border-radius: inherit; background: linear-gradient(90deg, #4bc99b, #0c8e6c); }
.level-rule { margin-top: 18rpx; padding: 16rpx 18rpx; border-radius: 14rpx; color: #8b6a3c; background: #fff8e8; font-size: 21rpx; line-height: 1.5; }
.network-card { padding: 0; overflow: hidden; border-color: #cde9df; background: linear-gradient(145deg, #f7fffb, #fff); }
.network-header { display: flex; min-height: 104rpx; align-items: center; justify-content: space-between; gap: 18rpx; padding: 24rpx 26rpx; box-sizing: border-box; }
.network-header:active { background: #f0faf6; }
.network-title-wrap { flex: 1; min-width: 0; }
.network-summary { margin-top: 7rpx; color: #789189; font-size: 21rpx; line-height: 1.4; }
.network-toggle { display: flex; flex: none; min-width: 108rpx; min-height: 72rpx; align-items: center; justify-content: center; gap: 8rpx; border: 1rpx solid #bfe5d8; border-radius: 16rpx; color: #0c8568; background: #f0faf6; font-size: 21rpx; font-weight: 700; }
.network-chevron { font-size: 27rpx; line-height: 1; transform: rotate(0deg); transition: transform .2s ease; }
.network-chevron.expanded { transform: rotate(180deg); }
.network-content { padding: 0 26rpx 26rpx; border-top: 1rpx solid #e1f0eb; }
.network-state { display: flex; min-height: 112rpx; align-items: center; justify-content: center; color: #81968f; font-size: 22rpx; text-align: center; }
.network-error { color: #b45e47; }
.network-legend { display: flex; flex-wrap: wrap; gap: 12rpx 24rpx; padding-top: 20rpx; color: #6e8880; font-size: 19rpx; }
.network-legend view { display: flex; align-items: center; gap: 8rpx; }
.legend-dot { display: inline-block; width: 14rpx; height: 14rpx; border-radius: 50%; }
.legend-dot.direct { background: #22a980; box-shadow: 0 0 0 5rpx #ddf5ec; }
.legend-dot.team { background: #d29a3b; box-shadow: 0 0 0 5rpx #fff2d9; }
.network-self-node { position: relative; display: flex; width: 180rpx; align-items: center; flex-direction: column; margin: 28rpx auto 0; }
.network-self-node::after { content: ''; width: 2rpx; height: 32rpx; margin-top: 8rpx; background: #8fd6c0; }
.network-avatar { display: flex; align-items: center; justify-content: center; border-radius: 50%; box-sizing: border-box; color: #fff; font-weight: 800; }
.network-avatar.self { width: 74rpx; height: 74rpx; border: 6rpx solid #d7f4e9; background: linear-gradient(145deg, #0c8066, #28bc91); font-size: 29rpx; box-shadow: 0 8rpx 18rpx rgba(13, 133, 104, .18); }
.network-avatar.direct { width: 62rpx; height: 62rpx; border: 5rpx solid #def4ec; background: #148c6e; font-size: 25rpx; }
.network-avatar.team { width: 46rpx; height: 46rpx; border: 4rpx solid #fff1d5; color: #8b621e; background: #f5c86e; font-size: 20rpx; }
.network-node-name { width: 100%; margin-top: 8rpx; overflow: hidden; color: #264e43; font-size: 21rpx; font-weight: 750; line-height: 1.35; text-align: center; text-overflow: ellipsis; white-space: nowrap; }
.network-node-level { margin-top: 3rpx; color: #80958e; font-size: 17rpx; line-height: 1.3; text-align: center; }
.network-scroll { width: 100%; overflow: hidden; }
.network-branches { position: relative; display: flex; min-height: 220rpx; align-items: flex-start; box-sizing: border-box; padding: 0 10rpx 10rpx; }
.network-branches::before { content: ''; position: absolute; top: 0; right: 92rpx; left: 92rpx; height: 2rpx; background: #8fd6c0; }
.network-branch { position: relative; flex: 0 0 224rpx; width: 224rpx; padding: 28rpx 10rpx 0; box-sizing: border-box; }
.network-stem { position: absolute; top: 0; left: 50%; width: 2rpx; height: 28rpx; background: #8fd6c0; }
.network-member-node { display: flex; align-items: center; flex-direction: column; }
.network-child-stem { width: 2rpx; height: 22rpx; margin: 8rpx auto 0; background: #e5bd70; }
.network-child-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12rpx 8rpx; padding-top: 12rpx; border-top: 2rpx solid #e5bd70; }
.network-child-node { display: flex; min-width: 0; align-items: center; flex-direction: column; }
.network-child-name { width: 100%; margin-top: 5rpx; overflow: hidden; color: #6e5c3d; font-size: 17rpx; text-align: center; text-overflow: ellipsis; white-space: nowrap; }
.network-hidden, .network-hidden-direct, .network-scroll-tip { color: #8a9b95; font-size: 18rpx; text-align: center; }
.network-hidden { margin-top: 10rpx; }
.network-hidden-direct { margin-top: 14rpx; color: #6f8980; }
.network-scroll-tip { margin-top: 12rpx; }
.network-empty { margin-top: -8rpx; padding: 26rpx 18rpx; border-radius: 16rpx; color: #80958e; background: #f3faf7; font-size: 21rpx; line-height: 1.5; text-align: center; }
.trend-card { border-color: #cce8df; background: linear-gradient(150deg, #fff, #f6fcf9); }
.trend-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 18rpx; }
.trend-period { margin-top: 7rpx; color: #81968f; font-size: 20rpx; }
.trend-current { flex: none; color: #789189; font-size: 20rpx; }
.trend-current text { margin-right: 4rpx; color: #0b8e6d; font-size: 36rpx; font-weight: 820; }
.trend-state { display: flex; min-height: 220rpx; align-items: center; justify-content: center; color: #879b94; font-size: 22rpx; }
.trend-legend { display: flex; align-items: center; justify-content: flex-end; gap: 8rpx; margin-top: 18rpx; color: #647f76; font-size: 19rpx; }
.trend-legend-line { display: inline-block; width: 30rpx; height: 4rpx; border-radius: 4rpx; background: #0d9672; box-shadow: 0 0 0 1rpx rgba(13, 150, 114, .1); }
.trend-chart { position: relative; height: 250rpx; margin: 10rpx 0 0 38rpx; }
.trend-grid-line { position: absolute; right: 4%; left: 4%; height: 2px; background: #deece7; }
.trend-grid-line text { position: absolute; top: -12rpx; left: -38rpx; width: 32rpx; color: #9aaba5; font-size: 16rpx; text-align: right; }
.trend-grid-line.top { top: calc(12% - 1px); }
.trend-grid-line.middle { top: calc(50% - 1px); }
.trend-grid-line.bottom { bottom: calc(8% - 1px); }
.trend-segment { position: absolute; z-index: 2; height: 2px; border-radius: 0; background: linear-gradient(90deg, #4ac89a, #0d8d6c); transform-origin: left center; }
.trend-point { position: absolute; z-index: 3; width: 0; height: 0; }
.trend-point-dot { position: absolute; top: -7rpx; left: -7rpx; width: 14rpx; height: 14rpx; border: 4rpx solid #d8f4e9; border-radius: 50%; box-sizing: border-box; background: #0c8e6c; box-shadow: 0 3rpx 8rpx rgba(12, 142, 108, .2); }
.trend-date-row { display: flex; box-sizing: border-box; margin: 2rpx 0 0 38rpx; padding: 0 4%; }
.trend-date-row text { flex: 1; color: #91a39d; font-size: 16rpx; text-align: center; }
.trend-summary { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; margin-top: 18rpx; padding-top: 16rpx; border-top: 1rpx solid #e2eee9; color: #5d7d73; font-size: 20rpx; }
.trend-summary text:first-child { color: #0c8568; font-weight: 700; }
.reward-row { display: flex; align-items: baseline; justify-content: space-between; gap: 12rpx; margin-top: 18rpx; }
.reward-main { color: #759089; font-size: 22rpx; }
.reward-number { color: #0a906e; font-size: 40rpx; font-weight: 800; }
.reward-label { color: #7a938c; font-size: 21rpx; }
.reward-track { margin-top: 14rpx; }
.protection-copy { margin-top: 16rpx; color: #69847a; font-size: 22rpx; line-height: 1.5; }
.protection-rule { margin-top: 10rpx; color: #8b9b95; font-size: 20rpx; line-height: 1.5; }
.register-card { border-color: #f0dda4; background: linear-gradient(135deg, #fffaf0, #fff); }
.register-copy, .region-copy, .referral-note { margin-top: 10rpx; color: #7e8e88; font-size: 22rpx; line-height: 1.55; }
.field-label { margin-top: 18rpx; color: #48665e; font-size: 23rpx; font-weight: 700; }
.register-mode { display: flex; gap: 14rpx; margin-top: 10rpx; }
.register-mode-option { display: flex; flex: 1; min-height: 88rpx; flex-direction: column; justify-content: center; padding: 0 18rpx; border: 2rpx solid #d8e9e2; border-radius: 16rpx; box-sizing: border-box; background: #fff; }
.register-mode-option.active { border-color: #0d8b6c; background: #effaf5; box-shadow: inset 0 0 0 1rpx #0d8b6c; }
.register-mode-title { color: #31584d; font-size: 25rpx; font-weight: 750; }
.register-mode-option.active .register-mode-title { color: #0d8064; }
.register-mode-note { margin-top: 5rpx; color: #8a9c95; font-size: 20rpx; }
.text-input { height: 78rpx; margin-top: 10rpx; padding: 0 20rpx; border: 2rpx solid #d8e9e2; border-radius: 16rpx; box-sizing: border-box; color: #23463c; background: #fff; font-size: 25rpx; }
.picker-field { display: flex; align-items: center; justify-content: space-between; min-height: 78rpx; margin-top: 10rpx; padding: 0 20rpx; border: 2rpx solid #d8e9e2; border-radius: 16rpx; box-sizing: border-box; color: #23463c; background: #fff; font-size: 25rpx; }
.picker-field.placeholder { color: #9aaea7; }
.picker-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.picker-arrow { margin-left: 12rpx; color: #0d8568; font-size: 38rpx; line-height: 1; }
.field-helper { margin-top: 8rpx; color: #81958e; font-size: 20rpx; line-height: 1.4; }
.register-error { color: #b45e47; }
.invite-label { color: #0d8064; }
.invite-input { border-color: #9bdcc8; background: #f8fffc; }
.invite-helper { color: #0d8064; }
.primary-button, .secondary-button { height: 84rpx; margin-top: 20rpx; border: 0; border-radius: 18rpx; font-size: 28rpx; font-weight: 750; line-height: 84rpx; }
.primary-button { color: #fff; background: #0d8568; }
.secondary-button { color: #0d8064; background: #ddf5eb; }
.primary-button[disabled], .secondary-button[disabled] { opacity: .55; }
.purchase-card { border-color: #b9e7d7; background: linear-gradient(145deg, #f2fcf8, #fff); }
.robot-card { border-color: #ecd8ac; background: linear-gradient(145deg, #fffaf0, #fff); }
.robot-card-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16rpx; }
.robot-count { flex: none; padding: 8rpx 12rpx; border-radius: 14rpx; color: #8b671e; background: #fff1c9; font-size: 20rpx; font-weight: 700; }
.robot-copy { margin-top: 14rpx; color: #6f8178; font-size: 22rpx; line-height: 1.6; }
.robot-error { color: #b45e47; }
.robot-button {
  margin-top: 22rpx;
  border: 2rpx solid #a9dac8;
  box-sizing: border-box;
  color: #176b56;
  background: #e6f7ef;
  box-shadow: 0 6rpx 14rpx rgba(19, 119, 88, .08);
}
.robot-button:active { background: #d4efe3; }
.robot-button[disabled] { opacity: .5; box-shadow: none; }
.robot-dialog-mask { position: fixed; z-index: 100; top: 0; right: 0; bottom: 0; left: 0; display: flex; align-items: center; justify-content: center; padding: 36rpx; box-sizing: border-box; background: rgba(21, 39, 34, .58); }
.robot-dialog { width: 100%; max-width: 650rpx; padding: 30rpx 28rpx 26rpx; border: 2rpx solid #ecd7a9; border-radius: 30rpx; box-sizing: border-box; background: #fffdf8; box-shadow: 0 22rpx 70rpx rgba(17, 54, 43, .24); }
.robot-dialog-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 18rpx; }
.robot-dialog-kicker { color: #b5792a; font-size: 20rpx; font-weight: 800; letter-spacing: 2rpx; }
.robot-dialog-title { margin-top: 8rpx; color: #25463c; font-size: 34rpx; font-weight: 820; }
.robot-dialog-close { display: flex; align-items: center; justify-content: center; width: 64rpx; height: 64rpx; border-radius: 50%; color: #7f8d86; background: #f3f0e8; font-size: 42rpx; line-height: 1; }
.robot-dialog-pill { display: inline-flex; margin-top: 18rpx; padding: 9rpx 14rpx; border-radius: 14rpx; color: #9b681f; background: #fff0c5; font-size: 21rpx; font-weight: 750; }
.robot-dialog-copy { margin-top: 16rpx; color: #687b72; font-size: 23rpx; line-height: 1.55; }
.robot-qr-frame { display: flex; align-items: center; justify-content: center; min-height: 410rpx; margin: 22rpx auto 0; padding: 18rpx; border: 1rpx solid #f0dfb8; border-radius: 22rpx; box-sizing: border-box; background: #fff; }
.robot-qr-image { width: 370rpx; height: 370rpx; }
.robot-qr-error { padding: 34rpx 24rpx; color: #b45e47; text-align: center; font-size: 22rpx; line-height: 1.55; }
.robot-qr-order-tip { margin-top: 16rpx; padding: 12rpx 18rpx; border-radius: 12rpx; color: #8b6b2f; background: #fff7e2; text-align: center; font-size: 22rpx; line-height: 1.45; }
.robot-qr-tip { margin-top: 12rpx; color: #919991; text-align: center; font-size: 20rpx; line-height: 1.45; }
.robot-dialog-actions { display: flex; gap: 14rpx; margin-top: 18rpx; }
.robot-action-button { flex: 1; min-width: 0; height: 92rpx; min-height: 92rpx; margin-top: 0; padding: 0 10rpx; font-size: 25rpx; line-height: 92rpx; }
.robot-action-hint { box-sizing: border-box; text-align: center; }
.robot-dialog-button { margin-top: 20rpx; background: #0d8568; }
.purchase-copy { margin-top: 10rpx; color: #6d8980; font-size: 22rpx; line-height: 1.55; }
.quantity-presets { display: flex; gap: 14rpx; margin-top: 12rpx; }
.quantity-preset { display: flex; flex: 1; align-items: center; justify-content: center; min-height: 88rpx; border: 2rpx solid #cfe9df; border-radius: 14rpx; color: #39776a; background: #fff; font-size: 24rpx; font-weight: 700; }
.quantity-preset.active { border-color: #0d8b6c; color: #fff; background: #0d8b6c; }
.quantity-input { margin-top: 14rpx; }
.purchase-total { margin-top: 18rpx; color: #718b83; text-align: right; font-size: 23rpx; }
.purchase-total text { margin-left: 8rpx; color: #0b8b6b; font-size: 34rpx; font-weight: 800; }
.purchase-button { margin-top: 14rpx; }
.referral-card { text-align: center; }
.referral-header { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.referral-card .card-heading { flex: 1; text-align: left; }
.referral-copy-hint { flex: 0 0 auto; color: #0c8568; font-size: 21rpx; font-weight: 700; line-height: 1.4; white-space: nowrap; }
.referral-code { display: block; margin-top: 18rpx; padding: 18rpx 16rpx; border: 2rpx dashed #8ed6c0; border-radius: 16rpx; color: #087d60; background: #f1fcf7; font-family: monospace; font-size: 34rpx; font-weight: 800; letter-spacing: 2rpx; line-height: 1.4; word-break: break-all; }
.settlement-card { border-color: #c8e9dc; background: linear-gradient(145deg, #f4fcf8, #fff); }
.settlement-header { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; }
.settlement-loading { color: #7b9b90; font-size: 20rpx; }
.settlement-copy { margin-top: 12rpx; color: #6d8980; font-size: 21rpx; line-height: 1.5; }
.settlement-row { display: flex; align-items: center; gap: 14rpx; padding: 18rpx 0 4rpx; border-bottom: 1rpx solid #e5f1ec; }
.settlement-row:last-child { border-bottom: 0; }
.settlement-main { flex: 1; min-width: 0; }
.settlement-title { color: #2d5d4e; font-size: 23rpx; font-weight: 750; }
.settlement-meta { margin-top: 5rpx; color: #78948a; font-size: 20rpx; line-height: 1.4; }
.settlement-actions { display: flex; flex: none; align-items: center; gap: 10rpx; }
.settlement-success { flex: none; color: #0d8b6c; font-size: 21rpx; font-weight: 700; }
.settlement-button { flex: none; min-width: 144rpx; height: 76rpx; min-height: 76rpx; margin: 0; padding: 0 12rpx; border: 1rpx solid #9bdcc8; border-radius: 14rpx; color: #0c8568; background: #effaf5; font-size: 20rpx; line-height: 74rpx; white-space: nowrap; }
.settlement-actions .settlement-button { width: 144rpx; min-width: 144rpx; height: 88rpx; min-height: 88rpx; padding: 0 8rpx; line-height: 86rpx; }
.settlement-button::after { border: 0; }
.secondary-settlement-button { border-color: #d2e7df; color: #5d8377; background: #f7fbf9; }
.auto-receive-inline-button { border-color: #b8dfd1; color: #087c61; background: #f8fcfa; }
.settlement-button[disabled] { opacity: .55; }
.region-current { margin-top: 16rpx; color: #0a8969; font-size: 28rpx; font-weight: 750; }
.ledger-header { display: flex; align-items: center; justify-content: space-between; }
.ledger-card .text-button { position: relative; display: flex; flex: 0 0 76rpx; align-items: center; justify-content: center; box-sizing: border-box; width: 76rpx; min-width: 76rpx; height: 76rpx; min-height: 76rpx; margin: 0; padding: 0; border: 1rpx solid #bfe7da; border-radius: 14rpx; color: #0c8568; background: #f3fcf8; font-size: 21rpx; font-weight: 700; line-height: 1; }
.ledger-card .text-button-label { position: absolute; top: 50%; left: 50%; line-height: 1.2; white-space: nowrap; transform: translate(-50%, -50%); }
.ledger-card .text-button-hover { opacity: .72; }
.ledger-card .text-button.disabled { opacity: .55; pointer-events: none; }
.ledger-collapsed { display: flex; min-height: 66rpx; align-items: center; justify-content: center; padding-top: 4rpx; color: #899d96; font-size: 21rpx; line-height: 1.4; }
.ledger-empty { padding: 28rpx 0 6rpx; color: #94a49e; text-align: center; font-size: 22rpx; }
.ledger-row { display: flex; align-items: center; gap: 18rpx; padding: 20rpx 0; border-bottom: 1rpx solid #edf2ef; }
.ledger-row:last-child { border-bottom: 0; }
.ledger-copy { flex: 1; min-width: 0; }
.ledger-title { overflow: hidden; color: #31554b; font-size: 23rpx; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }
.ledger-meta { margin-top: 5rpx; color: #97a7a2; font-size: 19rpx; }
.ledger-amount { flex: none; color: #0c936e; font-size: 28rpx; font-weight: 800; }
.ledger-amount.debit { color: #bd6b4f; }
.disclaimer { padding: 28rpx 10rpx 10rpx; color: #8b9b95; text-align: center; font-size: 20rpx; line-height: 1.55; }
.disabled-card { margin-top: 30rpx; padding: 40rpx 28rpx; border: 1rpx solid #dcebe6; border-radius: 28rpx; text-align: center; background: #fff; }
.disabled-title { color: #244a40; font-size: 34rpx; font-weight: 800; }
.disabled-copy { margin-top: 12rpx; color: #7f938c; font-size: 24rpx; line-height: 1.55; }
@media (max-width: 360px) { .hero-title { font-size: 40rpx; } .balance-value { font-size: 42rpx; } .trend-date-row text { font-size: 15rpx; } }
</style>
