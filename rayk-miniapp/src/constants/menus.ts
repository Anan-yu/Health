import type { Role } from '@/types/api'
import { goldBeanEnabled, mallEnabled } from '@/constants/features'

export interface MenuItem {
  title: string
  description: string
  icon: string
  route: string
  permission?: string
}

const customer: MenuItem[] = [
  {
    title: '健康档案',
    description: '维护个人基础健康资料',
    icon: '档',
    route: '/pages-customer/profile/index',
    permission: 'self:health-record',
  },
  {
    title: '健康检测',
    description: '通过面部影像完成健康检测',
    icon: '测',
    route: '/pages-customer/health-scan/index',
    permission: 'self:assessment',
  },
  {
    title: '健康助手',
    description: '结合本人资料解答健康问题',
    icon: '助',
    route: '/pages-customer/medical-assistant/index',
    permission: 'self:health-record',
  },
  {
    title: '上传检验报告',
    description: '上传报告并确认识别结果',
    icon: '传',
    route: '/pages-customer/lab-report/upload',
    permission: 'self:lab-report',
  },
  {
    title: '我的检验报告',
    description: '查看已上传的检验报告',
    icon: '报',
    route: '/pages-customer/lab-report/index',
    permission: 'self:lab-report',
  },
  {
    title: '健康总览',
    description: '查看综合解读、健康仪表盘和维度卡片',
    icon: '总',
    route: '/pages-customer/assessment/index',
    permission: 'self:assessment',
  },
  {
    title: '我的健康报告',
    description: '查看已生成的 PDF 健康报告',
    icon: '康',
    route: '/pages-customer/health-report/index',
    permission: 'self:health-report',
  },
  {
    title: '健康随访',
    description: '查看并完成健康随访任务',
    icon: '访',
    route: '/pages-customer/followup/index',
    permission: 'self:followup',
  },
  {
    title: '指标趋势',
    description: '查看历次指标变化趋势',
    icon: '趋',
    route: '/pages-customer/trend/index',
    permission: 'self:lab-report',
  },
  {
    title: '健康提醒',
    description: '设置吃饭与睡觉语音提醒',
    icon: '音',
    route: '/pages-customer/voice-reminder/index',
    permission: 'self:health-record',
  },
]

/**
 * Public service descriptions shown before a user chooses to log in.
 * These entries intentionally point at the authenticated routes, but the
 * guest pages intercept the tap and explain why login is needed instead of
 * sending an unauthenticated request.
 */
export const guestMenus: MenuItem[] = [
  {
    title: '健康档案',
    description: '了解如何整理个人基础健康资料',
    icon: '档',
    route: '/pages-customer/profile/index',
  },
  {
    title: '健康检测',
    description: '了解面部影像健康检测服务',
    icon: '测',
    route: '/pages-customer/health-scan/index',
  },
  {
    title: '健康助手',
    description: '了解结合个人资料的健康问答',
    icon: '助',
    route: '/pages-customer/medical-assistant/index',
  },
  {
    title: '上传检验报告',
    description: '了解报告上传和识别流程',
    icon: '传',
    route: '/pages-customer/lab-report/upload',
  },
  {
    title: '我的检验报告',
    description: '了解已上传检验报告的查看方式',
    icon: '报',
    route: '/pages-customer/lab-report/index',
  },
  {
    title: '健康总览',
    description: '了解指标整理与健康管理建议',
    icon: '评',
    route: '/pages-customer/assessment/index',
  },
  {
    title: '我的健康报告',
    description: '了解综合健康报告和阶段建议',
    icon: '康',
    route: '/pages-customer/health-report/index',
  },
  {
    title: '健康随访',
    description: '了解持续记录和行动反馈服务',
    icon: '访',
    route: '/pages-customer/followup/index',
  },
  {
    title: '指标趋势',
    description: '了解历次健康指标变化趋势',
    icon: '趋',
    route: '/pages-customer/trend/index',
  },
  {
    title: '健康提醒',
    description: '了解吃饭与睡眠语音提醒服务',
    icon: '音',
    route: '/pages-customer/voice-reminder/index',
  },
]

const doctor: MenuItem[] = [
  {
    title: '体检者查询',
    description: '按姓名或手机号查询本院已授权客户',
    icon: '查',
    route: '/pages-business/patient/index',
    permission: 'patient:list',
  },
]

const platform: MenuItem[] = [
  {
    title: '医院管理',
    description: '创建、编辑、启停合作医院和预录入医生',
    icon: '院',
    route: '/pages-tenant/dashboard/index',
    permission: 'platform:tenant:list',
  },
  {
    title: '累计随访任务',
    description: '查看任务、客户反馈与完成情况',
    icon: '访',
    route: '/pages-tenant/dashboard/followup',
    permission: 'platform:tenant:list',
  },
  {
    title: '会员管理',
    description: '按手机号开通或取消客户年度会员',
    icon: '会',
    route: '/pages-platform/membership/index',
    permission: 'platform:tenant:list',
  },
  {
    title: '金豆会员运营',
    description: '查看开发环境会员、推荐关系、费用归属与金豆流水',
    icon: '豆',
    route: '/pages-platform/gold-bean/index',
    permission: 'platform:tenant:list',
  },
  {
    title: '商城商品',
    description: '维护实物商品、价格、库存和上下架状态',
    icon: '商',
    route: '/pages-platform/mall/index',
    permission: 'platform:tenant:list',
  },
  {
    title: '反馈中心',
    description: '查看、回复与关闭用户反馈',
    icon: '答',
    route: '/pages-platform/support/index',
  },
]

export const menusFor = (role: Role | '') => {
  const menus =
    role === '' ? guestMenus : role === 'CUSTOMER' ? customer : role === 'PLATFORM_ADMIN' ? platform : doctor
  const mallMenus = mallEnabled ? menus : menus.filter((item) => item.route !== '/pages-platform/mall/index')
  return goldBeanEnabled
    ? mallMenus
    : mallMenus.filter((item) => item.route !== '/pages-platform/gold-bean/index')
}
