import type { Role } from '@/types/api'

export interface MenuItem { title: string; description: string; icon: string; route: string; permission?: string }

const customer: MenuItem[] = [
  { title: '健康档案', description: '维护个人基础健康资料', icon: '档', route: '/pages-customer/profile/index', permission: 'self:health-record' },
  { title: '上传检验报告', description: '上传报告并确认识别结果', icon: '传', route: '/pages-customer/lab-report/upload', permission: 'self:lab-report' },
  { title: '我的报告', description: '查看已上传的检验报告', icon: '报', route: '/pages-customer/lab-report/index', permission: 'self:lab-report' },
  { title: 'AI评估结果', description: '查看健康评估与建议', icon: 'AI', route: '/pages-customer/assessment/index', permission: 'self:assessment' },
  { title: '健康报告', description: '查看已生成的 PDF 健康报告', icon: '康', route: '/pages-customer/health-report/index', permission: 'self:health-report' },
  { title: '指标趋势', description: '查看历次指标变化趋势', icon: '趋', route: '/pages-customer/trend/index', permission: 'self:lab-report' },
  { title: '健康随访', description: '查看并完成健康随访任务', icon: '访', route: '/pages-customer/followup/index', permission: 'self:followup' },
]

const doctor: MenuItem[] = [
  { title: '体检者查询', description: '按姓名或手机号查询本院已授权客户', icon: '查', route: '/pages-business/patient/index', permission: 'patient:list' },
]

const platform: MenuItem[] = [
  { title: '医院管理', description: '创建、编辑、启停合作医院和预录入医生', icon: '院', route: '/pages-tenant/dashboard/index', permission: 'platform:tenant:list' },
  { title: '反馈中心', description: '查看、回复与关闭用户反馈', icon: '答', route: '/pages-platform/support/index' },
]

export const menusFor = (role: Role | '') =>
  role === 'CUSTOMER' ? customer : role === 'PLATFORM_ADMIN' ? platform : doctor
