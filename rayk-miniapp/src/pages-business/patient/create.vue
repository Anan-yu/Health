<template>
  <view class="page"
    ><view class="title">创建客户</view
    ><view class="card"
      ><input v-model="form.name" class="input" placeholder="客户姓名" /><picker
        :range="genders"
        @change="pickGender"
        ><view class="input">性别：{{ form.gender }}</view></picker
      ><input v-model="form.birthDate" class="input" placeholder="出生日期 YYYY-MM-DD" /><input
        v-model="form.phone"
        class="input"
        placeholder="手机号（服务端仅保存脱敏值）"
      /><input
        v-model="form.assignedDoctorId"
        class="input"
        placeholder="医生ID（演示：10003）"
      /><input
        v-model="form.assignedManagerId"
        class="input"
        placeholder="健康管理师ID（演示：10004）"
      /><button class="primary" :loading="loading" @click="submit">保存客户档案</button></view
    ></view
  >
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { createPatient } from '@/api/patient'
const genders = ['UNKNOWN', 'MALE', 'FEMALE']
const form = reactive({
    name: '新测试客户',
    gender: 'UNKNOWN',
    birthDate: '1990-01-01',
    phone: '13800001234',
    assignedDoctorId: '10003',
    assignedManagerId: '10004',
  }),
  loading = ref(false)
function pickGender(e: { detail: { value: number } }) {
  form.gender = genders[e.detail.value]
}
async function submit() {
  loading.value = true
  try {
    const item = await createPatient({
      ...form,
      assignedDoctorId: Number(form.assignedDoctorId),
      assignedManagerId: Number(form.assignedManagerId),
    })
    uni.redirectTo({ url: `/pages-business/patient/detail?id=${item.id}` })
  } finally {
    loading.value = false
  }
}
</script>
