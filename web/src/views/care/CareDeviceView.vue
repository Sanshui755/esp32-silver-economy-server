<template>
  <div class="care-device-view">
    <!-- 设备状态 -->
    <a-card :title="t('router.title.careDevice')" :bordered="false" style="margin-bottom: 16px">
      <template #extra>
        <a-button @click="loadDevices">{{ t('care.common.refresh') }}</a-button>
      </template>
      <a-table
        row-key="deviceId"
        :columns="deviceColumns"
        :data-source="deviceData"
        :loading="deviceLoading"
        :pagination="devicePagination"
        size="middle"
        @change="deviceTableChange"
      >
        <template #emptyText>
          <TableEmptyState :error="deviceLoadError" @retry="deviceRetry" />
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'online'">
            <a-badge :status="record.online === '1' ? 'success' : 'default'"
              :text="record.online === '1' ? t('care.device.online') : t('care.device.offline')" />
          </template>
          <template v-else-if="column.dataIndex === 'battery'">
            <template v-if="record.battery != null">
              <a-progress
                :percent="record.battery"
                :size="[80, 8]"
                :stroke-color="record.battery <= 20 ? 'red' : record.battery <= 40 ? 'orange' : '#52c41a'"
                :show-info="false"
              />
              <span style="margin-left: 6px">{{ record.battery }}%</span>
            </template>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'rssi'">
            <span v-if="record.rssi != null">{{ record.rssi }} dBm</span>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button
                v-permission="'system:care:command:api:send'"
                type="primary"
                size="small"
                @click="openMessage(record)"
              >{{ t('care.device.sendCareMessage') }}</a-button>
              <a-button
                v-permission="'system:care:command:api:send'"
                size="small"
                :loading="queryLoading"
                @click="queryStatus(record)"
              >{{ t('care.device.queryStatus') }}</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 命令记录 -->
    <a-card :title="t('care.device.commandLogs')" :bordered="false">
      <a-table
        row-key="id"
        :columns="logColumns"
        :data-source="logData"
        :loading="logLoading"
        :pagination="logPagination"
        size="middle"
        @change="logTableChange"
      >
        <template #emptyText>
          <TableEmptyState :error="logLoadError" @retry="logRetry" />
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'commandType'">
            {{ commandText(record.commandType) }}
          </template>
          <template v-else-if="column.dataIndex === 'payload'">
            <a-tooltip :title="record.payload" placement="top">
              <span class="ellipsis-text">{{ record.payload }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag v-if="record.status === 'acked'" color="green">{{ t('care.command.acked') }}</a-tag>
            <a-tag v-else-if="record.status === 'delivered'" color="blue">{{ t('care.command.delivered') }}</a-tag>
            <a-tag v-else-if="record.status === 'failed'" color="red">{{ t('care.command.failed') }}</a-tag>
            <a-tag v-else>{{ t('care.command.sent') }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 发送关怀消息 -->
    <a-modal
      v-model:open="messageOpen"
      :title="t('care.device.sendCareMessage')"
      :confirm-loading="sendLoading"
      @ok="confirmSend"
    >
      <a-alert v-if="targetDevice" :message="`${targetDevice.elderName || targetDevice.elderId}`" type="info" show-icon style="margin-bottom: 12px" />
      <a-form layout="vertical">
        <a-form-item :label="t('care.device.messageContent')" required>
          <a-textarea v-model:value="messageText" :rows="3" :placeholder="t('care.device.messagePlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'
import { useTable } from '@/composables/useTable'
import { useRequest } from '@/composables/useRequest'
import TableEmptyState from '@/components/TableEmptyState.vue'
import {
  queryCareDevices,
  queryCommandLogs,
  sendCareCommand,
} from '@/services/care'
import type { CareDevice, CommandLog } from '@/types/care'

const { t } = useI18n()

const {
  loading: deviceLoading, data: deviceData, pagination: devicePagination, loadError: deviceLoadError,
  onTableChange: deviceTableChange, retryLoad: deviceRetry, fetchData: loadDevices,
} = useTable<CareDevice>((params) => queryCareDevices(params as Record<string, unknown>))

const deviceColumns: TableColumnsType = [
  { title: t('care.elder.name'), dataIndex: 'elderName', width: 110 },
  { title: t('care.device.deviceId'), dataIndex: 'deviceId', width: 160 },
  { title: t('care.device.status'), dataIndex: 'online', width: 100 },
  { title: t('care.device.battery'), dataIndex: 'battery', width: 130 },
  { title: t('care.device.rssi'), dataIndex: 'rssi', width: 100 },
  { title: t('care.device.lastHeartbeat'), dataIndex: 'lastHeartbeat', width: 170 },
  { title: t('care.elder.action'), dataIndex: 'action', width: 220, fixed: 'right' },
]

const {
  loading: logLoading, data: logData, pagination: logPagination, loadError: logLoadError,
  onTableChange: logTableChange, retryLoad: logRetry, fetchData: loadLogs,
} = useTable<CommandLog>((params) => queryCommandLogs(params as Record<string, unknown>))

const logColumns: TableColumnsType = [
  { title: t('care.elder.name'), dataIndex: 'elderName', width: 110 },
  { title: t('care.command.type'), dataIndex: 'commandType', width: 120 },
  { title: t('care.command.payload'), dataIndex: 'payload', ellipsis: true },
  { title: t('care.command.status'), dataIndex: 'status', width: 100 },
  { title: t('care.command.createTime'), dataIndex: 'createTime', width: 170 },
]

function commandText(type: string) {
  const map: Record<string, string> = {
    care_message: t('care.command.careMessage'),
    query_status: t('care.device.queryStatus'),
    set_medication_plan: t('care.command.setPlan'),
    delete_medication_plan: t('care.command.deletePlan'),
    medication_reminder: t('care.event.medicationReminder'),
  }
  return map[type] || type
}

// 关怀消息
const messageOpen = ref(false)
const messageText = ref('')
const targetDevice = ref<CareDevice | null>(null)
const sendLoading = ref(false)
const queryLoading = ref(false)
const { executeOk } = useRequest()

function openMessage(record: CareDevice) {
  targetDevice.value = record
  messageText.value = ''
  messageOpen.value = true
}

async function confirmSend() {
  if (!targetDevice.value || !messageText.value.trim()) return
  sendLoading.value = true
  const ok = await executeOk(() =>
    sendCareCommand({
      elderId: targetDevice.value!.elderId || targetDevice.value!.deviceId,
      commandType: 'care_message',
      message: messageText.value.trim(),
    })
  )
  sendLoading.value = false
  if (ok) {
    message.success(t('common.success'))
    messageOpen.value = false
    loadLogs()
  }
}

async function queryStatus(record: CareDevice) {
  queryLoading.value = true
  const ok = await executeOk(() =>
    sendCareCommand({
      elderId: record.elderId || record.deviceId,
      commandType: 'query_status',
    })
  )
  queryLoading.value = false
  if (ok) {
    message.success(t('common.success'))
    loadLogs()
  }
}

onMounted(() => {
  loadDevices()
  loadLogs()
})
</script>

<style scoped lang="scss">
.care-device-view {
  padding: 24px;
}
</style>
