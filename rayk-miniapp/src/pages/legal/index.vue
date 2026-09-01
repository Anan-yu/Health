<template>
  <view class="page legal-page elder-page">
    <view class="legal-heading">
      <view class="legal-kicker">三羊健康</view>
      <view class="title">{{ currentDocument.title }}</view>
    </view>

    <view class="card legal-card">
      <view class="legal-notice">请在登录或使用需要授权的服务前认真阅读以下内容，并在登录页主动点击同意。未同意前，你仍可查看本页面条款。</view>
      <view v-for="(section, sectionIndex) in currentDocument.sections" :key="section.title" class="legal-section">
        <view class="section-title">{{ sectionIndex + 1 }}. {{ section.title }}</view>
        <view v-for="(paragraph, paragraphIndex) in section.paragraphs" :key="`${section.title}-${paragraphIndex}`" class="legal-paragraph">
          {{ paragraph }}
        </view>
      </view>
    </view>

    <view class="legal-switch">
      <text class="legal-switch-label">还要查看：</text>
      <text v-if="legalType === 'service'" class="legal-switch-link" @tap="switchDocument('privacy')">《隐私政策》</text>
      <text v-else class="legal-switch-link" @tap="switchDocument('service')">《用户服务协议》</text>
    </view>
    <view class="legal-footer">如需咨询、纠正或删除个人信息，请在小程序“帮助与反馈”中提交申请。</view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

type LegalType = 'service' | 'privacy'
type LegalSection = { title: string; paragraphs: string[] }
type LegalDocument = { title: string; sections: LegalSection[] }

const legalDocuments: Record<LegalType, LegalDocument> = {
  service: {
    title: '用户服务协议',
    sections: [
      {
        title: '协议的接受与适用范围',
        paragraphs: [
          '本协议由星序元科技（河南）有限公司（以下简称“我们”）与使用“三羊健康”微信小程序（以下简称“本小程序”）的用户共同订立。你在登录页主动点击同意并继续授权登录，即表示已阅读、理解并接受本协议。',
          '如果你不同意本协议或其中任何条款，请停止登录和使用相关服务。未成年人应在监护人指导下阅读并使用本小程序。',
        ],
      },
      {
        title: '我们提供的服务',
        paragraphs: [
          '本小程序主要提供健康档案维护、健康问卷、体检报告上传与查看、健康指标整理、健康评估参考、健康报告查看、健康随访、健康提醒、健康助手和问题反馈等健康管理服务。具体功能以当前页面实际开放情况为准。',
          '部分功能可能因账号身份、服务配置、网络状态、设备能力或合规要求而暂时不可用。我们会在必要时调整、暂停或停止部分服务，并尽量通过页面提示告知。',
        ],
      },
      {
        title: '账号、身份与使用安全',
        paragraphs: [
          '你应使用本人微信账号和真实、合法的授权信息登录，不得冒用他人身份、转让账号、绕过权限或协助他人访问不属于其本人或授权范围内的数据。',
          '你应妥善保管微信账号、设备和登录状态。如发现账号被盗用、授权异常或数据被他人访问，请立即通过“帮助与反馈”联系我们。因你主动泄露授权、设备失管或未及时通知造成的损失，应由你在法律允许范围内承担相应责任。',
        ],
      },
      {
        title: '健康信息与人工智能服务边界',
        paragraphs: [
          '本小程序中的健康评估、报告解读、健康助手和随访建议仅用于健康管理与辅助参考，不构成疾病诊断、治疗方案、处方、急救建议或医疗器械测量结果，不能替代医生面诊、临床检查或专业医疗意见。',
          '你应根据实际情况如实、完整地填写资料并核对报告内容。遇到急性症状、严重不适或紧急情况，请立即联系医疗机构或拨打当地急救电话，不要仅依赖本小程序的内容作出医疗决定。',
          '人工智能生成内容可能存在遗漏、延迟或不准确情形。你应结合原始检验结果和专业人员意见进行判断，并对基于相关内容作出的决定负责。',
        ],
      },
      {
        title: '用户行为与内容',
        paragraphs: [
          '你上传、填写或提交的内容应来源合法，不得包含恶意程序、违法信息、侵犯他人隐私或知识产权的内容，不得利用本小程序从事诈骗、骚扰、非法交易、虚假传播或其他违反法律法规的活动。',
          '如你提交的内容影响平台、其他用户或服务安全，我们可以在法律允许范围内采取提醒、限制功能、删除相关内容或终止服务等措施，并保留依法追究责任的权利。',
        ],
      },
      {
        title: '第三方服务与数据处理',
        paragraphs: [
          '为完成微信登录、文件存储、文字或图片识别、人工智能分析、语音提醒、消息通知等功能，本小程序可能使用经配置的第三方平台或受托服务商。我们会按照最小必要原则提供完成服务所需的信息，并要求受托方采取相应的安全保护措施。',
          '第三方服务可能有独立的服务条款和隐私规则。涉及第三方页面或能力时，请同时阅读其相关规则；第三方服务本身出现的故障、规则变化或不可归责于我们的中断，我们会在能力范围内协助处理。',
        ],
      },
      {
        title: '知识产权',
        paragraphs: [
          '本小程序的界面、文字、图片、程序、商标、服务标识和其他内容的相关权利归我们或合法权利人所有。未经书面许可，你不得复制、改编、反向工程、抓取、传播或用于本协议约定之外的商业用途。',
          '你保留对本人合法上传内容的权利，并授予我们为提供、维护、改进本小程序而在必要范围内使用、存储和处理该内容的非独占许可；该许可在服务所需期限内有效。',
        ],
      },
      {
        title: '协议变更、终止与责任范围',
        paragraphs: [
          '我们可能因服务内容、法律法规或安全要求变化而更新本协议，请您及时查看，重大变化会通过适当方式提示。继续使用服务即视为接受更新后的协议。',
          '在法律允许范围内，我们会对因网络、设备、第三方服务或不可抗力导致的暂时中断采取合理措施，但不承诺服务始终无错误、不中断或完全满足特定用途。法律规定不得限制或排除的责任不受本条影响。',
        ],
      },
      {
        title: '联系我们',
        paragraphs: [
          '如对本协议、服务内容或个人信息处理有疑问，可通过本小程序“帮助与反馈”提交问题。我们会在合理期限内核查并回复；涉及身份核验、数据安全或法律要求的事项，可能需要你提供必要的证明材料。',
        ],
      },
    ],
  },
  privacy: {
    title: '隐私政策',
    sections: [
      {
        title: '我们如何保护你的隐私',
        paragraphs: [
          '星序元科技（河南）有限公司重视你的个人信息和健康信息保护。本政策说明“三羊健康”微信小程序在提供服务过程中如何收集、使用、存储、共享和保护相关信息，以及你可以如何管理这些信息。',
          '健康信息、体检报告、面部健康检测结果等属于敏感个人信息。我们只在提供明确功能所必需的范围内处理，并通过页面提示、授权或其他法律要求的方式取得必要同意。',
        ],
      },
      {
        title: '我们可能收集的信息',
        paragraphs: [
          '为完成微信登录和身份识别，我们可能处理微信账号标识、登录凭证、授权手机号、昵称或头像等信息。开发调试环境还可能使用测试账号信息；测试数据与生产数据分开保存。',
          '为提供健康管理服务，我们可能处理你主动填写的姓名、性别、出生日期、身高、体重、腰围、既往史、家族史、生活方式、健康问卷、检验报告、图片或 PDF 文件、健康拍结果、健康评估、随访反馈、提醒设置和健康助手对话内容。',
          '为保障服务安全和排查故障，我们可能记录必要的设备、网络、操作日志、接口请求结果和异常信息。我们会尽量减少收集范围，不在普通日志中记录 API 密钥、完整身份证号、完整手机号、微信会话密钥或完整健康报告。',
        ],
      },
      {
        title: '我们如何使用这些信息',
        paragraphs: [
          '我们使用相关信息来完成登录、建立和维护健康档案、识别和整理体检报告、生成健康管理参考、提供随访和提醒、处理反馈、改善产品体验、保障账号与系统安全，以及履行法律法规要求。',
          '如果你拒绝提供某项功能所必需的信息，该功能可能无法使用，但不影响其他不依赖该信息的功能。我们不会将健康信息用于与所说明目的无关的营销或画像。',
        ],
      },
      {
        title: '敏感个人信息与单独管理',
        paragraphs: [
          '当你主动上传报告、填写健康资料、使用健康拍、健康助手或健康评估时，我们会处理相应的敏感健康信息。请在确认信息真实、理解用途和风险后再提交；你可以在“隐私授权”页面查看或撤回部分可撤回的授权。',
          '撤回授权不会影响撤回前基于授权已经进行的处理，但可能导致对应功能无法继续使用。依法需要保存的记录，我们会在法定期限内保留并采取访问控制。',
        ],
      },
      {
        title: '信息共享、委托处理与公开',
        paragraphs: [
          '在提供服务所必需的范围内，我们可能向微信登录、云存储、文字或图片识别、人工智能分析、语音合成、消息通知等受托服务商提供最少必要信息，并通过合同、权限控制和安全措施约束其处理行为。具体启用的服务以实际产品配置为准。',
          '在你的授权范围和岗位权限内，平台管理员或医生可能查看服务所需的健康档案、原始报告、健康评估、报告和随访信息；普通客户只能查看和操作本人数据。除法律法规要求、履行服务或保护安全所必需外，我们不会公开你的健康信息。',
          '如发生合并、分立、收购或其他导致个人信息控制者变化的情形，我们会依法告知并要求新的处理者继续履行本政策规定的保护义务。',
        ],
      },
      {
        title: '信息存储、安全与保存期限',
        paragraphs: [
          '你的账号和健康数据存储在受访问控制的服务端环境中，网络传输使用安全连接；原始报告和健康报告默认不公开，通过鉴权接口或短时授权地址访问。我们会根据数据敏感程度采取权限、审计、备份和异常监控等措施。',
          '我们会在实现服务所必需的期限内保存信息，并按照法律法规、争议处理、审计和安全要求保留必要记录。超过保存期限或你依法提出删除请求后，我们会删除或匿名化处理；法律另有规定的除外。',
          '如果发生可能影响个人信息安全的事件，我们会按照法律法规采取补救、通知和报告措施。',
        ],
      },
      {
        title: '你可以如何管理个人信息',
        paragraphs: [
          '你可以在小程序中查看或更新可编辑的健康档案，管理隐私授权，查看本人报告、随访和反馈记录，并通过“帮助与反馈”提出访问、复制、更正、删除、撤回同意或解释说明请求。',
          '为保护数据安全，我们可能先核验你的身份。对无法立即处理或依法不能满足的请求，我们会说明原因和可行的替代方式。',
        ],
      },
      {
        title: '未成年人、跨境与政策更新',
        paragraphs: [
          '本小程序不以未成年人为主要服务对象。未成年人使用前应取得监护人同意并在监护人指导下进行；如监护人发现未成年人未经同意提交个人信息，可通过“帮助与反馈”联系我们。',
          '我们会根据实际服务和法律法规变化更新本政策，并在本页面展示新的生效日期。涉及重要变化时，我们会通过适当方式提示。',
        ],
      },
      {
        title: '联系我们',
        paragraphs: [
          '如对个人信息处理有疑问、投诉或需要行使权利，请在小程序“帮助与反馈”中提交申请，并尽量说明涉及的功能和诉求。我们会在合理期限内处理并回复。',
        ],
      },
    ],
  },
}

const legalType = ref<LegalType>('service')
const currentDocument = computed(() => legalDocuments[legalType.value])

function switchDocument(type: LegalType) {
  legalType.value = type
  uni.setNavigationBarTitle({ title: currentDocument.value.title })
  uni.pageScrollTo({ scrollTop: 0, duration: 0 })
}

onLoad((query) => {
  legalType.value = query?.type === 'privacy' ? 'privacy' : 'service'
  uni.setNavigationBarTitle({ title: currentDocument.value.title })
})
</script>

<style scoped>
.legal-heading {
  margin-bottom: 28rpx;
  padding: 8rpx 8rpx 0;
}
.legal-kicker {
  margin-bottom: 8rpx;
  color: #0f7a62;
  font-size: 24rpx;
  font-weight: 750;
  letter-spacing: 2rpx;
}
.legal-heading .title {
  margin-bottom: 8rpx;
}
.legal-card {
  padding: 32rpx 30rpx 38rpx;
}
.legal-notice {
  margin-bottom: 30rpx;
  padding: 22rpx 24rpx;
  border: 2rpx solid #cdece0;
  border-radius: 22rpx;
  color: #35665a;
  background: #f2fcf7;
  font-size: 27rpx;
  line-height: 1.7;
}
.legal-section + .legal-section {
  margin-top: 32rpx;
  padding-top: 28rpx;
  border-top: 2rpx solid #eaf1ee;
}
.legal-section .section-title {
  margin: 0 0 12rpx;
  color: #173e34;
  font-size: 32rpx;
  line-height: 1.45;
  font-weight: 780;
}
.legal-paragraph {
  margin-top: 14rpx;
  color: #435f56;
  font-size: 29rpx;
  line-height: 1.85;
  text-align: left;
  word-break: break-word;
}
.legal-switch {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4rpx;
  min-height: 88rpx;
  margin-top: 10rpx;
  color: #71857d;
  font-size: 27rpx;
}
.legal-switch-label {
  display: inline-flex;
  align-items: center;
  min-height: 88rpx;
}
.legal-switch-link {
  display: inline-flex;
  align-items: center;
  min-height: 88rpx;
  padding: 0 8rpx;
  color: #0f7a62;
  font-weight: 750;
  text-decoration: none;
}
.legal-footer {
  padding: 12rpx 18rpx 0;
  color: #869890;
  font-size: 24rpx;
  line-height: 1.7;
  text-align: center;
}
</style>
