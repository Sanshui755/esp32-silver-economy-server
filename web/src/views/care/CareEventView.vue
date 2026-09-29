<template>
  <div class="care-event-view">
    <!-- 筛选 -->
    <a-card :bordered="false" class="search-card">
      <a-form layout="inline">
        <a-form-item :label="t('care.event.eventType')">
          <a-select
            v-model:value="filterType"
            style="width: 160px"
            allow-clear
            :placeholder="t('care.event.allTypes')"
            @change="debouncedSearch"
          >
            <a-select-option value="fall">{{ t('care.event.fall') }}</a-select-option>
            <a-select-option value="sos">{{ t('care.event.sos') }}</a-select-option>
            <a-select-option value="medication_reminder">{{ t('care.event.medicationReminder') }}</a-select-option>
            <a-select-option value="medication_taken">{{ t('care.event.medicationTaken') }}</a-select-option>
            <a-select-option value="device_offline">{{ t('care.event.deviceOffline') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="t('care.event.severity')">
          <a-select v-model:value="filterSeverity" style="width: 120px" allow-clear @change="debouncedSearch">
            <a-select-option value="critical">{{ t('care.severity.critical') }}</a-select-option>
            <a-select-option value="warning">{{ t('care.severity.warning') }}</a-select-option>
            <a-select-option value="info">{{ t('care.severity.info') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="t('care.event.status')">
          <a-select v-model:value="filterStatus" style="width: 120px" allow-clear @change="debouncedSearch">
            <a-select-option value="0">{{ t('care.event.unhandled') }}</a-select-option>
            <a-select-option value="1">{{ t('care.event.handled') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button @click="fetchData">{{ t('care.common.refresh') }}</a-button>
        </a-form-item>
        <a-form-item>
          <a-badge :status="sseConnected ? 'success' : 'error'" :text="sseConnected ? t('care.common.live') : t('care.common.disconnected')" />
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 事件列表 -->
    <a-card :title="t('router.title.careEvent')" :bordered="false">
      <a-table
        row-key="id"
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        size="middle"
        @change="onTableChange"
      >
        <template #emptyText>
          <TableEmptyState :error="loadError" @retry="retryLoad" />
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'eventType'">
            <a-tag :color="typeColor(record.eventType)">{{ typeName(record.eventType) }}</a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'severity'">
            <a-tag v-if="record.severity === 'critical'" color="red">{{ t('care.severity.critical') }}</a-tag>
            <a-tag v-else-if="record.severity === 'warning'" color="orange">{{ t('care.severity.warning') }}</a-tag>
            <a-tag v-else>{{ t('care.severity.info') }}</a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'status'">
            <a-tag v-if="record.status === '1'" color="green">{{ t('care.event.handled') }}</a-tag>
            <a-tag v-else color="volcano">{{ t('care.event.unhandled') }}</a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button size="small" @click="showDetail(record)">{{ t('care.event.detail') }}</a-button>
              <a-button
                v-if="record.status === '0'"
                v-permission="'system:care:event:api:handle'"
                type="primary"
                size="small"
                :loading="handleLoading"
                @click="confirmHandle(record)"
              >{{ t('care.event.markHandled') }}</a-button>
            </a-space>
          </template>

          <template v-else-if="column.dataIndex === 'payload'">
            <a-tooltip :title="record.payload" placement="top">
              <span class="ellipsis-text">{{ briefPayload(record.payload) }}</span>
            </a-tooltip>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 事件详情 -->
    <a-modal v-model:open="detailOpen" :title="t('care.event.detail')" :footer="null" width="640px">
      <a-descriptions v-if="detailEvent" :column="1" bordered size="small">
        <a-descriptions-item :label="t('care.event.eventType')">{{ typeName(detailEvent.eventType) }}</a-descriptions-item>
        <a-descriptions-item :label="t('care.elder.name')">{{ detailEvent.elderName || detailEvent.elderId }}</a-descriptions-item>
        <a-descriptions-item :label="t('care.event.severity')">{{ detailEvent.severity }}</a-descriptions-item>
        <a-descriptions-item :label="t('care.event.source')">{{ detailEvent.source }}</a-descriptions-item>
        <a-descriptions-item :label="t('care.event.createTime')">{{ detailEvent.createTime }}</a-descriptions-item>
        <a-descriptions-item :label="t('care.event.payload')">
          <pre class="payload-pre">{{ prettyPayload(detailEvent.payload) }}</pre>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import { useTable } from '@/composables/useTable'
import { useRequest } from '@/composables/useRequest'
import TableEmptyState from '@/components/TableEmptyState.vue'
import { handleEvent, queryEvents } from '@/services/care'
import { closeCareSse, openCareSse } from '@/services/careSse'
import type { CareEvent, CareEventQueryParams } from '@/types/care'

const { t } = useI18n()

const filterType = ref<string>()
const filterSeverity = ref<string>()
const filterStatus = ref<string>()
const sseConnected = ref(false)

function buildQueryParams(): Partial<CareEventQueryParams> {
  return {
    eventType: filterType.value,
    severity: filterSeverity.value,
    status: filterStatus.value,
  }
}

const {
  loading, data, pagination, loadError,
  onTableChange, debouncedSearch, retryLoad, fetchData,
} = useTable<CareEvent, Partial<CareEventQueryParams>>(
  (params) => queryEvents(params as Partial<CareEventQueryParams>),
  buildQueryParams
)

const columns: TableColumnsType = [
  { title: t('care.elder.name'), dataIndex: 'elderName', width: 110 },
  { title: t('care.event.eventType'), dataIndex: 'eventType', width: 130 },
  { title: t('care.event.severity'), dataIndex: 'severity', width: 90 },
  { title: t('care.event.payload'), dataIndex: 'payload', ellipsis: true },
  { title: t('care.event.status'), dataIndex: 'status', width: 90 },
  { title: t('care.event.createTime'), dataIndex: 'createTime', width: 170 },
  { title: t('care.elder.action'), dataIndex: 'action', width: 180, fixed: 'right' },
]

const { executeOk, loading: handleLoading } = useRequest()

async function confirmHandle(record: CareEvent) {
  const ok = await executeOk(() => handleEvent(record.id))
  if (ok) {
    message.success(t('common.success'))
    fetchData()
  }
}

// 详情
const detailOpen = ref(false)
const detailEvent = ref<CareEvent | null>(null)
function showDetail(record: CareEvent) {
  detailEvent.value = record
  detailOpen.value = true
}

function typeName(type: string) {
  const map: Record<string, string> = {
    fall: t('care.event.fall'),
    sos: t('care.event.sos'),
    medication_reminder: t('care.event.medicationReminder'),
    medication_taken: t('care.event.medicationTaken'),
    device_offline: t('care.event.deviceOffline'),
    heartbeat: t('care.event.heartbeat'),
    device_status: t('care.event.deviceStatus'),
    command_ack: t('care.event.commandAck'),
  }
  return map[type] || type
}

function typeColor(type: string) {
  return { fall: 'red', sos: 'red', medication_reminder: 'orange', medication_taken: 'green', device_offline: 'default' }[type] || 'blue'
}

function briefPayload(payload?: string) {
  if (!payload) return '-'
  try {
    const obj = JSON.parse(payload)
    const parts: string[] = []
    if (obj.battery != null) parts.push(`电量 ${obj.battery}%`)
    if (obj.location) parts.push(obj.location)
    if (obj.confidence != null) parts.push(`置信度 ${obj.confidence}`)
    if (obj.planId) parts.push(`计划 ${obj.planId}`)
    if (obj.medicine) parts.push(obj.medicine)
    return parts.length ? parts.join(' · ') : payload
  } catch {
    return payload
  }
}

function prettyPayload(payload?: string) {
  if (!payload) return '-'
  try {
    return JSON.stringify(JSON.parse(payload), null, 2)
  } catch {
    return payload
  }
}

// SSE 实时推送：新事件到达即刷新当前页
let sseStarted = false
function startSse() {
  if (sseStarted) return
  sseStarted = true
  openCareSse(
    () => fetchData(),
    () => { sseConnected.value = false }
  )
  // connected 事件没法直接探测，短暂后视为在线（断线由 onError 置回）
  window.setTimeout(() => { sseConnected.value = true }, 1500)
}

onMounted(() => {
  fetchData()
  startSse()
})

onUnmounted(() => {
  closeCareSse()
  sseStarted = false
})
</script>

<style scoped lang="scss">
.care-event-view {
  padding: 24px;
}
.search-card {
  margin-bottom: 16px;
}
.payload-pre {
  max-height: 300px;
  overflow: auto;
  margin: 0;
  font-size: 12px;
}
</style>
