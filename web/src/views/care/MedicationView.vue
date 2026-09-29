<template>
  <div class="medication-view">
    <a-card :title="t('router.title.careMedication')" :bordered="false">
      <template #extra>
        <a-space>
          <a-button @click="switchTab(t('care.medication.records'))">{{ activeTab === 'plans' ? t('care.medication.records') : t('care.medication.plans') }}</a-button>
          <a-button
            v-if="activeTab === 'plans'"
            v-permission="'system:care:medication:api:create'"
            type="primary"
            @click="openCreate"
          >{{ t('care.medication.create') }}</a-button>
        </a-space>
      </template>

      <!-- 计划列表 -->
      <template v-if="activeTab === 'plans'">
        <a-table
          row-key="id"
          :columns="planColumns"
          :data-source="planData"
          :loading="planLoading"
          :pagination="planPagination"
          size="middle"
          @change="planTableChange"
        >
          <template #emptyText>
            <TableEmptyState :error="planLoadError" @retry="planRetry" />
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'takeTime'">
              {{ (record.takeTime || '').slice(0, 5) }}
            </template>
            <template v-else-if="column.dataIndex === 'repeatRule'">
              {{ repeatText(record) }}
            </template>
            <template v-else-if="column.dataIndex === 'state'">
              <a-tag v-if="record.state === '1'" color="green">{{ t('care.medication.enabled') }}</a-tag>
              <a-tag v-else color="default">{{ t('care.medication.disabled') }}</a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-space>
                <a-button
                  v-permission="'system:care:medication:api:update'"
                  size="small"
                  @click="toggleState(record)"
                >{{ record.state === '1' ? t('care.medication.disable') : t('care.medication.enable') }}</a-button>
                <a-button
                  v-permission="'system:care:medication:api:update'"
                  size="small"
                  @click="openEdit(record)"
                >{{ t('common.edit') }}</a-button>
                <a-popconfirm :title="t('care.medication.deleteConfirm')" @confirm="confirmDelete(record)">
                  <a-button v-permission="'system:care:medication:api:delete'" size="small" danger>
                    {{ t('common.delete') }}
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </template>

      <!-- 执行记录 -->
      <template v-else>
        <a-table
          row-key="id"
          :columns="recordColumns"
          :data-source="recordData"
          :loading="recordLoading"
          :pagination="recordPagination"
          size="middle"
          @change="recordTableChange"
        >
          <template #emptyText>
            <TableEmptyState :error="recordLoadError" @retry="recordRetry" />
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'status'">
              <a-tag v-if="record.status === 'taken'" color="green">{{ t('care.medication.taken') }}</a-tag>
              <a-tag v-else-if="record.status === 'missed'" color="red">{{ t('care.medication.missed') }}</a-tag>
              <a-tag v-else color="orange">{{ t('care.medication.pending') }}</a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'confirmSource'">
              {{ sourceText(record.confirmSource) }}
            </template>
          </template>
        </a-table>
      </template>
    </a-card>

    <!-- 新增/编辑计划 -->
    <a-modal
      v-model:open="editOpen"
      :title="editingId ? t('care.medication.edit') : t('care.medication.create')"
      :confirm-loading="saveLoading"
      @ok="confirmSave"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('care.elder.name')" required>
          <a-select
            v-model:value="form.elderId"
            :options="elderOptions"
            show-search
            option-filter-prop="label"
            :placeholder="t('care.medication.selectElder')"
          />
        </a-form-item>
        <a-form-item :label="t('care.medication.medicine')" required>
          <a-input v-model:value="form.medicine" placeholder="降压药" />
        </a-form-item>
        <a-form-item :label="t('care.medication.dosage')">
          <a-input v-model:value="form.dosage" placeholder="1片" />
        </a-form-item>
        <a-form-item :label="t('care.medication.takeTime')" required>
          <a-time-picker v-model:value="takeTimeValue" format="HH:mm" value-format="HH:mm" style="width: 100%" />
        </a-form-item>
        <a-form-item :label="t('care.medication.repeatRule')">
          <a-radio-group v-model:value="form.repeatRule">
            <a-radio value="daily">{{ t('care.medication.daily') }}</a-radio>
            <a-radio value="weekdays">{{ t('care.medication.weekdays') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="t('care.medication.voicePrompt')">
          <a-textarea v-model:value="form.voicePrompt" :rows="2" :placeholder="t('care.medication.voicePromptPlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import { useTable } from '@/composables/useTable'
import { useRequest } from '@/composables/useRequest'
import TableEmptyState from '@/components/TableEmptyState.vue'
import {
  createMedicationPlan,
  deleteMedicationPlan,
  queryElders,
  queryMedicationPlans,
  queryMedicationRecords,
  updateMedicationPlan,
} from '@/services/care'
import type { Elder, MedicationPlan, MedicationRecord } from '@/types/care'

const { t } = useI18n()

const activeTab = ref<'plans' | 'records'>('plans')
function switchTab(tab: string) {
  activeTab.value = tab === t('care.medication.records') ? 'records' : 'plans'
}

// 计划列表
const {
  loading: planLoading, data: planData, pagination: planPagination, loadError: planLoadError,
  onTableChange: planTableChange, retryLoad: planRetry, fetchData: loadPlans,
} = useTable<MedicationPlan>((params) => queryMedicationPlans(params as Record<string, unknown>))

const planColumns: TableColumnsType = [
  { title: t('care.elder.name'), dataIndex: 'elderName', width: 110 },
  { title: t('care.medication.medicine'), dataIndex: 'medicine', width: 130 },
  { title: t('care.medication.dosage'), dataIndex: 'dosage', width: 90 },
  { title: t('care.medication.takeTime'), dataIndex: 'takeTime', width: 90 },
  { title: t('care.medication.repeatRule'), dataIndex: 'repeatRule', width: 110 },
  { title: t('care.medication.voicePrompt'), dataIndex: 'voicePrompt', ellipsis: true },
  { title: t('care.medication.stateLabel'), dataIndex: 'state', width: 80 },
  { title: t('care.elder.action'), dataIndex: 'action', width: 230, fixed: 'right' },
]

// 执行记录
const {
  loading: recordLoading, data: recordData, pagination: recordPagination, loadError: recordLoadError,
  onTableChange: recordTableChange, retryLoad: recordRetry, fetchData: loadRecords,
} = useTable<MedicationRecord>((params) => queryMedicationRecords(params as Record<string, unknown>))

const recordColumns: TableColumnsType = [
  { title: t('care.elder.name'), dataIndex: 'elderName', width: 110 },
  { title: t('care.medication.medicine'), dataIndex: 'medicine', width: 130 },
  { title: t('care.medication.plannedTime'), dataIndex: 'plannedTime', width: 160 },
  { title: t('care.medication.statusLabel'), dataIndex: 'status', width: 90 },
  { title: t('care.medication.takenTime'), dataIndex: 'takenTime', width: 160 },
  { title: t('care.medication.confirmSource'), dataIndex: 'confirmSource', width: 110 },
]

function sourceText(source?: string) {
  const map: Record<string, string> = {
    elder_device: t('care.medication.fromDevice'),
    family_app: t('care.medication.fromFamily'),
    server: t('care.medication.fromServer'),
  }
  return source ? (map[source] || source) : '-'
}

function repeatText(record: MedicationPlan) {
  if (record.repeatRule === 'weekdays') return t('care.medication.weekdays')
  return t('care.medication.daily')
}

// 新增/编辑
const editOpen = ref(false)
const editingId = ref<number>()
const saveLoading = ref(false)
const takeTimeValue = ref<string>()
const form = reactive<Partial<MedicationPlan>>({})
const elderOptions = ref<{ label: string; value: string }[]>([])
const { executeOk } = useRequest()

async function loadElderOptions() {
  const res = await queryElders({ pageNo: 1, pageSize: 100 })
  elderOptions.value = (res.data?.list || []).map((e: Elder) => ({ label: e.name, value: e.elderId }))
}

function openCreate() {
  editingId.value = undefined
  Object.assign(form, { elderId: undefined, medicine: '', dosage: '', repeatRule: 'daily', voicePrompt: '', state: '1' })
  takeTimeValue.value = '08:00'
  loadElderOptions()
  editOpen.value = true
}

function openEdit(record: MedicationPlan) {
  editingId.value = record.id
  Object.assign(form, record)
  takeTimeValue.value = (record.takeTime || '').slice(0, 5)
  loadElderOptions()
  editOpen.value = true
}

async function confirmSave() {
  if (!form.elderId || !form.medicine || !takeTimeValue.value) {
    message.warning(t('care.medication.requiredMissing'))
    return
  }
  const payload = { ...form, takeTime: `${takeTimeValue.value}:00` }
  saveLoading.value = true
  const ok = editingId.value
    ? await executeOk(() => updateMedicationPlan(payload))
    : await executeOk(() => createMedicationPlan(payload))
  saveLoading.value = false
  if (ok) {
    message.success(t('common.success'))
    editOpen.value = false
    loadPlans()
  }
}

async function toggleState(record: MedicationPlan) {
  const ok = await executeOk(() =>
    updateMedicationPlan({ id: record.id, state: record.state === '1' ? '0' : '1' } as Partial<MedicationPlan>)
  )
  if (ok) {
    message.success(t('common.success'))
    loadPlans()
  }
}

async function confirmDelete(record: MedicationPlan) {
  const ok = await executeOk(() => deleteMedicationPlan(record.id))
  if (ok) {
    message.success(t('common.success'))
    loadPlans()
  }
}

onMounted(() => {
  loadPlans()
  loadRecords()
})
</script>

<style scoped lang="scss">
.medication-view {
  padding: 24px;
}
</style>
