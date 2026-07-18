import type { Role } from '@/types/api'

export interface MenuItem {
  title: string
  description: string
  route: string
  permission?: string
}
const customer: MenuItem[] = [
  {
    title: '健康档案',
    description: '维护个人基础健康资料',
    route: '/pages-customer/profile/index',
    permission: 'self:health-record',
  },
  {
    title: '上传检验报告',
    description: '上传并确认模拟OCR结果',
    route: '/pages-customer/lab-report/upload',
    permission: 'self:lab-report',
  },
  {
    title: '我的报告',
    description: '查看检验和已发布报告',
    route: '/pages-customer/lab-report/index',
  },
  {
    title: 'AI评估结果',
    description: '查看已完成的演示评估',
    route: '/pages-customer/assessment/index',
  },
  { title: '随访中心', description: '查看任务并填写反馈', route: '/pages-customer/followup/index' },
]
const business: MenuItem[] = [
  {
    title: '客户管理',
    description: '卡片式客户档案管理',
    route: '/pages-business/patient/index',
    permission: 'patient:list',
  },
  {
    title: '报告任务',
    description: '报告、OCR与指标确认',
    route: '/pages-business/lab-report/index',
    permission: 'lab-report:manage',
  },
  {
    title: 'AI评估任务',
    description: '查看处理状态和证据',
    route: '/pages-business/assessment/index',
  },
  {
    title: '医生审核',
    description: '审核、退回和发布',
    route: '/pages-business/review/index',
    permission: 'assessment:review',
  },
  {
    title: '随访任务',
    description: '跟进健康管理执行情况',
    route: '/pages-business/followup/index',
  },
]
const tenant: MenuItem[] = [
  ...business,
  { title: '机构信息', description: '机构资料和员工', route: '/pages-tenant/profile/index' },
  { title: '员工管理', description: '机构人员与角色', route: '/pages-tenant/staff/index' },
]
const platform: MenuItem[] = [
  { title: '平台概览', description: '一期基础查看能力', route: '/pages-tenant/dashboard/index' },
  { title: '跨租户审计', description: '受控查看审计记录', route: '/pages-tenant/audit/index' },
]
export const menusFor = (role: Role | '') =>
  role === 'CUSTOMER'
    ? customer
    : role === 'TENANT_ADMIN'
      ? tenant
      : role === 'PLATFORM_ADMIN'
        ? platform
        : business
