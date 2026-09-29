<template>
  <div class="elder-view">
    <!-- 查询条件 -->
    <a-card :bordered="false" class="search-card">
      <a-form layout="inline">
        <a-form-item :label="t('care.elder.name')">
          <a-input-search
            v-model:value="searchName"
            :placeholder="t('care.elder.namePlaceholder')"
            style="width: 200px"
            allow-clear
            @search="debouncedSearch"
          />
        </a-form-item>
        <a-form-item :label="t('care.elder.phone')">
          <a-input-search
            v-model:value="searchPhone"
            :placeholder="t('care.elder.phonePlaceholder')"
            style="width: 180px"
            allow-clear
            @search="debouncedSearch"
          />
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 老人列表 -->
    <a-card :title="t('router.title.careElder')" :bordered="false">
      <template #extra>
        <a-button v-permission="'system:care:elder:api:create'" type="primary" @click="openCreate">
          {{ t('care.elder.create') }}
        </a-button>
      </template>

      <a-table
        row-key="elderId"
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
          <template v-if="column.dataIndex === 'name'">
            <span>{{ record.name }}</span>
            <a-tag v-if="record.gender === '1'" color="blue" style="margin-left: 6px">{{ t('care.elder.male') }}</a-tag>
            <a-tag v-else color="pink" style="margin-left: 6px">{{ t('care.elder.female') }}</a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'deviceOnline'">
            <template v-if="record.deviceId">
              <a-tag v-if="record.deviceOnline === '1'" color="green">{{ t('care.device.online') }}</a-tag>
              <a-tag v-else color="default">{{ t('care.device.offline') }}</a-tag>
            </template>
            <span v-else>-</span>
          </template>

          <template v-else-if="column.dataIndex === 'deviceBattery'">
            <span v-if="record.deviceBattery != null">{{ record.deviceBattery }}%</span>
            <span v-else>-</span>
          </template>

          <template v-else-if="column.dataIndex === 'bindings'">
            <a-tag
              v-for="b in (record.bindings || [])"
              :key="b.id"
              style="margin-bottom: 2px"
            >{{ b.nickname || b.username || b.familyId }}</a-tag>
            <span v-if="!(record.bindings && record.bindings.length)">-</span>
          </template>

          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button
                v-permission="'system:care:binding:api:list'"
                size="small"
                @click="openBindings(record)"
              >{{ t('care.binding.manage') }}</a-button>
              <a-button
                v-permission="'system:care:elder:api:update'"
                size="small"
                @click="openEdit(record)"
              >{{ t('common.edit') }}</a-button>
              <a-popconfirm
                :title="t('care.elder.deleteConfirm')"
                @confirm="confirmDelete(record)"
              >
                <a-button
                  v-permission="'system:care:elder:api:delete'"
                  size="small"
                  danger
                >{{ t('common.delete') }}</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑老人 -->
    <a-modal
      v-model:open="editOpen"
      :title="editingId ? t('care.elder.edit') : t('care.elder.create')"
      :confirm-loading="saveLoading"
      @ok="confirmSave"
    >
      <a-form layout="vertical">
        <a-form-item :label="t('care.elder.name')" required>
          <a-input v-model:value="form.name" :placeholder="t('care.elder.namePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('care.elder.gender')">
          <a-radio-group v-model:value="form.gender">
            <a-radio value="1">{{ t('care.elder.male') }}</a-radio>
            <a-radio value="0">{{ t('care.elder.female') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="t('care.elder.birthday')">
          <a-input v-model:value="form.birthday" placeholder="1948-06-01" />
        </a-form-item>
        <a-form-item :label="t('care.elder.phone')">
          <a-input v-model:value="form.phone" />
        </a-form-item>
        <a-form-item :label="t('care.elder.address')">
          <a-input v-model:value="form.address" />
        </a-form-item>
        <a-form-item :label="t('care.elder.deviceId')">
          <a-input v-model:value="form.deviceId" placeholder="esp32-xxxx（老年端设备ID）" />
        </a-form-item>
        <a-form-item :label="t('care.elder.healthNote')">
          <a-textarea v-model:value="form.healthNote" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 绑定管理 -->
    <a-modal v-model:open="bindingOpen" :title="t('care.binding.manage')" :footer="null" width="640px">
      <a-alert v-if="bindingElder" :message="`${bindingElder.name}（${bindingElder.elderId}）`" type="info" show-icon style="margin-bottom: 12px" />
      <div style="display: flex; gap: 8px; margin-bottom: 12px">
        <a-input-number
          v-model:value="bindUserId"
          :min="1"
          :placeholder="t('care.binding.userIdPlaceholder')"
          style="flex: 1"
        />
        <a-select v-model:value="bindRelation" style="width: 120px">
          <a-select-option value="son">{{ t('care.binding.son') }}</a-select-option>
          <a-select-option value="daughter">{{ t('care.binding.daughter') }}</a-select-option>
          <a-select-option value="other">{{ t('care.binding.other') }}</a-select-option>
        </a-select>
        <a-button
          v-permission="'system:care:binding:api:create'"
          type="primary"
          :loading="bindLoading"
          @click="confirmBind"
        >{{ t('care.binding.add') }}</a-button>
      </div>

      <a-list :data-source="bindingList" row-key="id" size="small" bordered>
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <span>{{ item.nickname || item.username || item.familyId }}</span>
                <a-tag v-if="item.isPrimary === '1'" color="gold" style="margin-left: 6px">{{ t('care.binding.primary') }}</a-tag>
              </template>
              <template #description>
                {{ t('care.binding.account') }}: {{ item.username || item.userId }} ·
                {{ t('care.binding.relation') }}: {{ item.relation || '-' }}
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-popconfirm :title="t('care.binding.unbindConfirm')" @confirm="confirmUnbind(item)">
                <a-button v-permission="'system:care:binding:api:delete'" size="small" danger>
                  {{ t('care.binding.unbind') }}
                </a-button>
              </a-popconfirm>
            </template>
          </a-list-item>
        </template>
      </a-list>
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
  bindFamily,
  createElder,
  deleteElder,
  queryBindings,
  queryElders,
  updateBinding,
  updateElder,
} from '@/services/care'
import type { Binding, Elder, ElderQueryParams } from '@/types/care'

const { t } = useI18n()

const searchName = ref('')
const searchPhone = ref('')

function buildQueryParams(): Partial<ElderQueryParams> {
  return { name: searchName.value || undefined, phone: searchPhone.value || undefined }
}

const {
  loading, data, pagination, loadError,
  onTableChange, debouncedSearch, retryLoad, fetchData,
} = useTable<Elder, Partial<ElderQueryParams>>(
  (params) => queryElders(params as Partial<ElderQueryParams>),
  buildQueryParams
)

const columns: TableColumnsType = [
  { title: t('care.elder.name'), dataIndex: 'name', width: 160 },
  { title: t('care.elder.birthday'), dataIndex: 'birthday', width: 110 },
  { title: t('care.elder.phone'), dataIndex: 'phone', width: 140 },
  { title: t('care.device.status'), dataIndex: 'deviceOnline', width: 100 },
  { title: t('care.device.battery'), dataIndex: 'deviceBattery', width: 90 },
  { title: t('care.binding.bindings'), dataIndex: 'bindings' },
  { title: t('care.elder.action'), dataIndex: 'action', width: 240, fixed: 'right' },
]

// 新增/编辑
const editOpen = ref(false)
const editingId = ref<string>('')
const saveLoading = ref(false)
const form = reactive<Partial<Elder>>({})
const { executeOk } = useRequest()

function openCreate() {
  editingId.value = ''
  Object.assign(form, { elderId: undefined, name: '', gender: '1', birthday: '', phone: '', address: '', deviceId: '', healthNote: '' })
  editOpen.value = true
}

function openEdit(record: Elder) {
  editingId.value = record.elderId
  Object.assign(form, record)
  editOpen.value = true
}

async function confirmSave() {
  if (!form.name) {
    message.warning(t('care.elder.namePlaceholder'))
    return
  }
  saveLoading.value = true
  const payload = { ...form }
  const ok = editingId.value
    ? await executeOk(() => updateElder(payload))
    : await executeOk(() => createElder(payload))
  saveLoading.value = false
  if (ok) {
    message.success(t('common.success'))
    editOpen.value = false
    fetchData()
  }
}

async function confirmDelete(record: Elder) {
  const ok = await executeOk(() => deleteElder(record.elderId))
  if (ok) {
    message.success(t('common.success'))
    fetchData()
  }
}

// 绑定管理
const bindingOpen = ref(false)
const bindingElder = ref<Elder | null>(null)
const bindingList = ref<Binding[]>([])
const bindUserId = ref<number>()
const bindRelation = ref('son')
const bindLoading = ref(false)

function openBindings(record: Elder) {
  bindingElder.value = record
  bindUserId.value = undefined
  bindingOpen.value = true
  void refreshBindings(record.elderId)
}

async function refreshBindings(elderId: string) {
  const res = await queryBindings(elderId)
  bindingList.value = res.data || []
}

async function confirmBind() {
  if (!bindingElder.value || !bindUserId.value) return
  bindLoading.value = true
  const ok = await executeOk(() =>
    bindFamily({ elderId: bindingElder.value!.elderId, userId: bindUserId.value!, relation: bindRelation.value })
  )
  bindLoading.value = false
  if (ok) {
    message.success(t('common.success'))
    void refreshBindings(bindingElder.value.elderId)
    fetchData()
  }
}

async function confirmUnbind(item: Binding) {
  const ok = await executeOk(() => updateBinding({ id: item.id, state: '0' }))
  if (ok) {
    message.success(t('common.success'))
    if (bindingElder.value) void refreshBindings(bindingElder.value.elderId)
    fetchData()
  }
}

onMounted(() => fetchData())
</script>

<style scoped lang="scss">
.elder-view {
  padding: 24px;
}
.search-card {
  margin-bottom: 16px;
}
</style>
