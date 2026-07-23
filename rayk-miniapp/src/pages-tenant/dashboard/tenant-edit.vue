<template>
  <view class="page admin-page">
    <view class="page-heading">
      <view class="eyebrow">HOSPITAL MANAGEMENT</view>
      <view class="title">编辑合作医院</view>
      <view class="subtitle">平台统一维护医院资料，并预录入医生信息以便微信手机号自动识别。</view>
    </view>
    <PageState :loading="loading" :error="error" :empty="!form">
      <view v-if="form" class="card form-card">
        <view class="field-label">医院名称</view>
        <input v-model="form.tenantName" class="input" maxlength="100" placeholder="请输入医院名称" />
        <view class="field-label">服务套餐</view>
        <input v-model="form.servicePlan" class="input" maxlength="50" placeholder="例如：STANDARD" />
        <view class="field-label">医院状态</view>
        <picker :range="statusOptions" range-key="label" @change="changeStatus">
          <view class="status-picker"><text>{{ selectedStatus.label }}</text><text>选择 ›</text></view>
        </picker>
        <view v-if="formError" class="form-error">{{ formError }}</view>
        <button class="primary save-button" :loading="saving" @click="save">保存医院信息</button>
      </view>

      <view class="staff-heading"><view>医生管理</view><text>{{ doctors.length }} 位</text></view>
      <view class="card staff-card">
        <view class="staff-tip">预录入的医生以手机号完成微信授权后，将自动进入本院查询工作台；可随时修改姓名或更换手机号。</view>
        <input v-model="doctorForm.displayName" class="input" maxlength="50" placeholder="医生姓名" />
        <input v-model="doctorForm.phone" class="input staff-input" type="number" maxlength="11" placeholder="医生手机号" />
        <button class="secondary staff-save" :loading="doctorSaving" @click="saveDoctor">预录入医生</button>
      </view>
      <view v-for="doctor in doctors" :key="doctor.id" class="card doctor-row">
        <view class="doctor-avatar">医</view>
        <view class="doctor-info"><view class="doctor-name">{{ doctor.displayName }}</view><view class="doctor-phone">{{ doctor.phoneMasked }}</view></view>
        <text class="doctor-tag">已预录入</text>
        <view class="doctor-actions"><text @click="startEditDoctor(doctor)">修改</text><text class="delete-action" @click="removeDoctor(doctor)">删除</text></view>
        <view v-if="editingDoctorId === doctor.id" class="doctor-edit">
          <input v-model="editDoctorForm.displayName" class="input" maxlength="50" placeholder="医生姓名" />
          <input v-model="editDoctorForm.phone" class="input staff-input" type="number" maxlength="11" placeholder="如需更换手机号，请输入新号码" />
          <view class="edit-tip">不填写新手机号时，将保留当前手机号。</view>
          <view class="edit-actions"><button class="secondary edit-cancel" @click="cancelEditDoctor">取消</button><button class="primary edit-save" :loading="doctorSaving" @click="updateDoctor(doctor.id)">保存修改</button></view>
        </view>
      </view>
    </PageState>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createPlatformDoctor, deletePlatformDoctor, getPlatformDoctors, getPlatformTenant, updatePlatformDoctor, updatePlatformTenant } from '@/api/admin'
import type { TenantStaff, UpdatePlatformTenantPayload } from '@/types/api'
import PageState from '@/components/PageState.vue'

const tenantId = ref(''), loading = ref(true), saving = ref(false), doctorSaving = ref(false), error = ref(''), formError = ref('')
const form = ref<UpdatePlatformTenantPayload>()
const doctors = ref<TenantStaff[]>([])
const doctorForm = reactive({ displayName: '', phone: '' })
const editingDoctorId = ref('')
const editDoctorForm = reactive({ displayName: '', phone: '' })
const statusOptions = [{ label: '正常服务', value: 'ACTIVE' as const }, { label: '暂停服务', value: 'DISABLED' as const }]
const selectedStatus = computed(() => statusOptions.find((item) => item.value === form.value?.status) || statusOptions[0])

async function load() {
  if (!tenantId.value) { error.value = '未找到医院编号'; loading.value = false; return }
  loading.value = true; error.value = ''
  try {
    const [tenant, staff] = await Promise.all([getPlatformTenant(tenantId.value), getPlatformDoctors(tenantId.value)])
    form.value = { tenantName: tenant.name, servicePlan: tenant.servicePlan, status: tenant.status === 'DISABLED' ? 'DISABLED' : 'ACTIVE' }
    doctors.value = staff
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '医院信息加载失败' }
  finally { loading.value = false }
}
function changeStatus(event: { detail: { value: number | string } }) { if (form.value) form.value.status = statusOptions[Number(event.detail.value)].value }
async function save() {
  if (!form.value) return
  const tenantName = form.value.tenantName.trim(), servicePlan = form.value.servicePlan.trim()
  if (!tenantName || !servicePlan) { formError.value = '请填写医院名称和服务套餐'; return }
  saving.value = true; formError.value = ''
  try { await updatePlatformTenant(tenantId.value, { ...form.value, tenantName, servicePlan }); uni.showToast({ title: '已保存', icon: 'success' }) }
  catch (cause) { formError.value = cause instanceof Error ? cause.message : '保存失败，请稍后重试' }
  finally { saving.value = false }
}
async function saveDoctor() {
  const displayName = doctorForm.displayName.trim(), phone = doctorForm.phone.trim()
  if (!displayName || !/^1[3-9]\d{9}$/.test(phone)) { uni.showToast({ title: '请输入医生姓名和有效手机号', icon: 'none' }); return }
  doctorSaving.value = true
  try { await createPlatformDoctor(tenantId.value, { displayName, phone }); doctorForm.displayName = ''; doctorForm.phone = ''; await load(); uni.showToast({ title: '医生已预录入', icon: 'success' }) }
  catch (cause) { uni.showToast({ title: cause instanceof Error ? cause.message : '预录入失败', icon: 'none' }) }
  finally { doctorSaving.value = false }
}
function startEditDoctor(doctor: TenantStaff) {
  editingDoctorId.value = doctor.id
  editDoctorForm.displayName = doctor.displayName
  editDoctorForm.phone = ''
}
function cancelEditDoctor() {
  editingDoctorId.value = ''
  editDoctorForm.displayName = ''
  editDoctorForm.phone = ''
}
async function updateDoctor(doctorId: string) {
  const displayName = editDoctorForm.displayName.trim(), phone = editDoctorForm.phone.trim()
  if (!displayName || (phone && !/^1[3-9]\d{9}$/.test(phone))) {
    uni.showToast({ title: '请填写医生姓名；更换手机号时请输入有效号码', icon: 'none' })
    return
  }
  doctorSaving.value = true
  try {
    await updatePlatformDoctor(tenantId.value, doctorId, { displayName, phone })
    cancelEditDoctor()
    await load()
    uni.showToast({ title: '医生信息已更新', icon: 'success' })
  } catch (cause) { uni.showToast({ title: cause instanceof Error ? cause.message : '修改失败，请稍后重试', icon: 'none' }) }
  finally { doctorSaving.value = false }
}
async function removeDoctor(doctor: TenantStaff) {
  const confirmed = await new Promise<boolean>((resolve) => uni.showModal({ title: '删除医生', content: `确定删除“${doctor.displayName}”吗？删除后该手机号将无法登录医生工作台。`, confirmColor: '#c63f32', success: (result) => resolve(result.confirm), fail: () => resolve(false) }))
  if (!confirmed) return
  doctorSaving.value = true
  try {
    await deletePlatformDoctor(tenantId.value, doctor.id)
    if (editingDoctorId.value === doctor.id) cancelEditDoctor()
    await load()
    uni.showToast({ title: '医生已删除', icon: 'success' })
  } catch (cause) { uni.showToast({ title: cause instanceof Error ? cause.message : '删除失败，请稍后重试', icon: 'none' }) }
  finally { doctorSaving.value = false }
}
onLoad((query) => { tenantId.value = typeof query?.id === 'string' ? query.id : ''; void load() })
</script>

<style scoped>
.admin-page { padding-top: 34rpx; }.page-heading { margin: 0 6rpx 28rpx; }.page-heading .title { margin-top: 8rpx; }.form-card,.staff-card { padding: 30rpx; }.field-label { margin: 25rpx 0 12rpx; color: #294a41; font-size: 25rpx; font-weight: 650; }.field-label:first-child { margin-top: 0; }.status-picker { display:flex; justify-content:space-between; padding:23rpx 24rpx; border:1rpx solid #dce9e4; border-radius:16rpx; background:#f8fbfa; color:#36564d; font-size:27rpx; }.save-button { margin-top: 30rpx; }.form-error { margin-top:16rpx;color:#b42318;font-size:23rpx; }.staff-heading { display:flex;justify-content:space-between;margin:34rpx 8rpx 16rpx;color:#21453a;font-size:30rpx;font-weight:700; }.staff-heading text { color:#789087;font-size:22rpx;font-weight:400; }.staff-tip { margin-bottom:20rpx;color:#71877f;font-size:22rpx;line-height:1.6; }.staff-input { margin-top:16rpx; }.staff-save { margin-top:20rpx; }.doctor-row { display:flex;flex-wrap:wrap;align-items:center;margin-top:16rpx;padding:24rpx; }.doctor-avatar { display:flex;align-items:center;justify-content:center;width:64rpx;height:64rpx;border-radius:20rpx;background:#dff3ec;color:#0f7a62;font-weight:700; }.doctor-info { min-width:0;flex:1; }.doctor-name { margin-left:18rpx;color:#24463c;font-size:28rpx;font-weight:650; }.doctor-phone { margin-left:18rpx;margin-top:6rpx;color:#82948d;font-size:22rpx; }.doctor-tag { margin-left:auto;padding:8rpx 14rpx;border-radius:999rpx;background:#e7f7f0;color:#0b7a5d;font-size:20rpx; }.doctor-actions { display:flex;gap:18rpx;margin-left:18rpx;color:#0d765e;font-size:23rpx;white-space:nowrap; }.doctor-actions .delete-action { color:#c63f32; }.doctor-edit { width:100%;margin-top:22rpx;padding-top:22rpx;border-top:1rpx solid #e3eee9; }.edit-tip { margin-top:10rpx;color:#82948d;font-size:21rpx; }.edit-actions { display:flex;gap:16rpx;margin-top:20rpx; }.edit-actions button { flex:1;margin:0;font-size:24rpx; }.edit-cancel { color:#315d51;background:#eef6f3; }.edit-save { min-width:0; }
</style>
