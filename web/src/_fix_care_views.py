#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""修复 4 个 care 视图对 useTable/useRequest 真实 API 的调用（一次性脚本，执行后删除）"""
import io
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)) + '/..')
BASE = 'src/views/care/'

def patch(path, pairs):
    s = io.open(path, encoding='utf-8').read()
    for old, new in pairs:
        assert old in s, f'{path}: pattern not found: {old[:60]!r}'
        s = s.replace(old, new)
    io.open(path, 'w', encoding='utf-8', newline='').write(s)
    print('patched', path)

# ---------- CareEventView.vue ----------
patch(BASE + 'CareEventView.vue', [
    ("""function buildQueryParams() {
  return {
    elderId: undefined,
    eventType: filterType.value,
    severity: filterSeverity.value,
    status: filterStatus.value,
  }
}""",
     """function buildQueryParams(): Partial<CareEventQueryParams> {
  return {
    eventType: filterType.value,
    severity: filterSeverity.value,
    status: filterStatus.value,
  }
}"""),
    ("""const {
  loading, data, pagination, loadError,
  onTableChange, debouncedSearch, retryLoad, loadData,
} = useTable<CareEvent, ReturnType<typeof buildQueryParams>>(
  (params) => queryEvents(params),
  buildQueryParams
)""",
     """const {
  loading, data, pagination, loadError,
  onTableChange, debouncedSearch, retryLoad, fetchData,
} = useTable<CareEvent, Partial<CareEventQueryParams>>(
  (params) => queryEvents(params as Partial<CareEventQueryParams>),
  buildQueryParams
)"""),
    ("const { run: runHandle, loading: handleLoading } = useRequest()",
     "const { execute, loading: handleLoading } = useRequest()"),
    ("await runHandle(() => handleEvent(record.id))", "await execute(() => handleEvent(record.id))"),
    ("  loadData()", "  fetchData()"),
    ("    () => loadData(),", "    () => fetchData(),"),
    ("import type { CareEvent } from '@/types/care'",
     "import type { CareEvent, CareEventQueryParams } from '@/types/care'"),
])

# ---------- MedicationView.vue ----------
patch(BASE + 'MedicationView.vue', [
    ("""// 计划列表
const planTable = useTable<MedicationPlan>((params) => queryMedicationPlans(params))
const { data: planData, loadData: loadPlans } = planTable""",
     """// 计划列表
const {
  loading: planLoading, data: planData, pagination: planPagination, loadError: planLoadError,
  onTableChange: planTableChange, retryLoad: planRetry, fetchData: loadPlans,
} = useTable<MedicationPlan>((params) => queryMedicationPlans(params as Record<string, unknown>))"""),
    ("""const recordTable = useTable<MedicationRecord>((params) => queryMedicationRecords(params))
const { data: recordData, loadData: loadRecords } = recordTable""",
     """const {
  loading: recordLoading, data: recordData, pagination: recordPagination, loadError: recordLoadError,
  onTableChange: recordTableChange, retryLoad: recordRetry, fetchData: loadRecords,
} = useTable<MedicationRecord>((params) => queryMedicationRecords(params as Record<string, unknown>))"""),
    (':loading="planTable.loading.value"', ':loading="planLoading"'),
    (':pagination="planTable.pagination.value"', ':pagination="planPagination"'),
    ('@change="planTable.onTableChange"', '@change="planTableChange"'),
    (':error="planTable.loadError.value" @retry="planTable.retryLoad"', ':error="planLoadError" @retry="planRetry"'),
    (':loading="recordTable.loading.value"', ':loading="recordLoading"'),
    (':pagination="recordTable.pagination.value"', ':pagination="recordPagination"'),
    ('@change="recordTable.onTableChange"', '@change="recordTableChange"'),
    (':error="recordTable.loadError.value" @retry="recordTable.retryLoad"', ':error="recordLoadError" @retry="recordRetry"'),
    ('const { run: runSave } = useRequest()', 'const { execute } = useRequest()'),
    ('await runSave(', 'await execute('),
])

# ---------- CareDeviceView.vue ----------
patch(BASE + 'CareDeviceView.vue', [
    ("""const deviceTable = useTable<CareDevice>((params) => queryCareDevices(params))
const { data: deviceData, loadData: loadDevices } = deviceTable""",
     """const {
  loading: deviceLoading, data: deviceData, pagination: devicePagination, loadError: deviceLoadError,
  onTableChange: deviceTableChange, retryLoad: deviceRetry, fetchData: loadDevices,
} = useTable<CareDevice>((params) => queryCareDevices(params as Record<string, unknown>))"""),
    ("""const logTable = useTable<CommandLog>((params) => queryCommandLogs(params))
const { data: logData, loadData: loadLogs } = logTable""",
     """const {
  loading: logLoading, data: logData, pagination: logPagination, loadError: logLoadError,
  onTableChange: logTableChange, retryLoad: logRetry, fetchData: loadLogs,
} = useTable<CommandLog>((params) => queryCommandLogs(params as Record<string, unknown>))"""),
    (':loading="deviceTable.loading.value"', ':loading="deviceLoading"'),
    (':pagination="deviceTable.pagination.value"', ':pagination="devicePagination"'),
    ('@change="deviceTable.onTableChange"', '@change="deviceTableChange"'),
    (':error="deviceTable.loadError.value" @retry="deviceTable.retryLoad"', ':error="deviceLoadError" @retry="deviceRetry"'),
    (':loading="logTable.loading.value"', ':loading="logLoading"'),
    (':pagination="logTable.pagination.value"', ':pagination="logPagination"'),
    ('@change="logTable.onTableChange"', '@change="logTableChange"'),
    (':error="logTable.loadError.value" @retry="logTable.retryLoad"', ':error="logLoadError" @retry="logRetry"'),
    ('const { run: runSend } = useRequest()', 'const { execute } = useRequest()'),
    ('await runSend(', 'await execute('),
])

# ---------- services/care.ts ----------
p = 'src/services/care.ts'
s = io.open(p, encoding='utf-8').read()
s = s.replace("import type { ApiResponse } from './request'\n", "")
s = s.replace("\nexport type { ApiResponse }\n", "\n")
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('patched', p)
