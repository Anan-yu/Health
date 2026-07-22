import type { Role } from '@/types/api'

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
    title: '上传检验报告',
    description: '上传报告并确认指标结果',
    icon: '传',
    route: '/pages-customer/lab-report/upload',
    permission: 'self:lab-report',
  },
  {
    title: '我的报告',
    description: '管理上传的原始检验报告',
    icon: '报',
    route: '/pages-customer/lab-report/index',
    permission: 'self:lab-report',
  },
  {
    title: 'AI评估结果',
    description: '查看健康风险与管理建议',
    icon: 'AI',
    route: '/pages-customer/assessment/index',
    permission: 'self:assessment',
  },
  {
    title: '健康报告',
    description: '查看医生发布的 PDF 健康报告',
    icon: '康',
    route: '/pages-customer/health-report/index',
    permission: 'self:health-report',
  },
  {
    title: '指标趋势',
    description: '查看历次确认指标的变化趋势',
    icon: '趋',
    route: '/pages-customer/trend/index',
    permission: 'self:lab-report',
  },
  {
    title: '随访中心',
    description: '查看任务并填写反馈',
    icon: '访',
    route: '/pages-customer/followup/index',
    permission: 'self:followup',
  },
]
const business: MenuItem[] = [
  {
    title: '客户管理',
    description: '卡片式客户档案管理',
    icon: '客',
    route: '/pages-business/patient/index',
    permission: 'patient:list',
  },
  {
    title: '报告任务',
    description: '查看客户报告与处理进度',
    icon: '报',
    route: '/pages-business/lab-report/index',
    permission: 'lab-report:manage',
  },
  {
    title: 'AI评估任务',
    description: '查看处理状态和证据',
    icon: 'AI',
    route: '/pages-business/assessment/index',
    permission: 'assessment:list',
  },
  {
    title: '医生审核',
    description: '审核、退回和发布',
    icon: '审',
    route: '/pages-business/review/index',
    permission: 'assessment:review',
  },
  {
    title: '随访任务',
    description: '跟进健康管理执行情况',
    icon: '访',
    route: '/pages-business/followup/index',
    permission: 'followup:manage',
  },
]
const tenant: MenuItem[] = [
  ...business,
  {
    title: '机构信息',
    description: '机构资料和员工',
    icon: '机',
    route: '/pages-tenant/profile/index',
    permission: 'tenant:staff:manage',
  },
  {
    title: '员工管理',
    description: '机构人员与角色',
    icon: '员',
    route: '/pages-tenant/staff/index',
    permission: 'tenant:staff:manage',
  },
]
const platform: MenuItem[] = [
  {
    title: '平台概览',
    description: '查看平台运营概况',
    icon: '总',
    route: '/pages-tenant/dashboard/index',
    permission: 'platform:tenant:list',
  },
  {
    title: '反馈中心',
    description: '统一查看、回复与关闭用户反馈',
    icon: '答',
    route: '/pages-platform/support/index',
  },
]
export const menusFor = (role: Role | '') =>
  role === 'CUSTOMER'
    ? customer
    : role === 'TENANT_ADMIN'
      ? tenant
      : role === 'PLATFORM_ADMIN'
        ? platform
        : business
