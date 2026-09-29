import { http } from './request'
import api from './api'
import type {
  Binding,
  CareDevice,
  CareEvent,
  CareEventQueryParams,
  CareDeviceQueryParams,
  CommandLog,
  CommandLogQueryParams,
  Elder,
  ElderQueryParams,
  MedicationPlan,
  MedicationRecord,
  MedicationRecordQueryParams,
} from '@/types/care'

/**
 * 老年关怀模块 API
 * 后端接口见 xiaozhi-server com.xiaozhi.care.controller
 */

// ---------------- 老人管理 ----------------

export function queryElders(params: Partial<ElderQueryParams>) {
  return http.getPage<Elder>(api.care.elder, params)
}

export function getElder(elderId: string) {
  return http.get<Elder>(`${api.care.elder}/${elderId}`)
}

export function createElder(data: Partial<Elder>) {
  return http.post<Elder>(api.care.elder, data)
}

export function updateElder(data: Partial<Elder>) {
  return http.patch<Elder>(api.care.elder, data)
}

export function deleteElder(elderId: string) {
  return http.delete(`${api.care.elder}/${elderId}`)
}

// ---------------- 子女绑定 ----------------

export function queryBindings(elderId: string) {
  return http.getList<Binding>(`${api.care.elder}/${elderId}/binding`)
}

export function bindFamily(data: { elderId: string; userId: number; relation?: string; isPrimary?: string }) {
  return http.post<Binding>(`${api.care.elder}/binding`, data)
}

export function updateBinding(data: { id: number; state?: string; isPrimary?: string }) {
  return http.patch<void>(`${api.care.elder}/binding`, data)
}

// ---------------- 事件中心 ----------------

export function queryEvents(params: Partial<CareEventQueryParams>) {
  return http.getPage<CareEvent>(api.care.event, params)
}

export function getEvent(id: number) {
  return http.get<CareEvent>(`${api.care.event}/${id}`)
}

export function handleEvent(id: number) {
  return http.patch<void>(`${api.care.event}/${id}/handle`)
}

// ---------------- 吃药提醒 ----------------

export function queryMedicationPlans(params: { pageNo?: number; pageSize?: number; elderId?: string }) {
  return http.getPage<MedicationPlan>(api.care.medicationPlan, params)
}

export function createMedicationPlan(data: Partial<MedicationPlan>) {
  return http.post<MedicationPlan>(api.care.medicationPlan, data)
}

export function updateMedicationPlan(data: Partial<MedicationPlan>) {
  return http.patch<MedicationPlan>(api.care.medicationPlan, data)
}

export function deleteMedicationPlan(id: number) {
  return http.delete(`${api.care.medicationPlan}/${id}`)
}

export function queryMedicationRecords(params: Partial<MedicationRecordQueryParams>) {
  return http.getPage<MedicationRecord>(api.care.medicationRecord, params)
}

// ---------------- 设备状态与命令 ----------------

export function queryCareDevices(params: Partial<CareDeviceQueryParams>) {
  return http.getPage<CareDevice>(api.care.device, params)
}

export function sendCareCommand(data: { elderId: string; commandType: string; message?: string; extra?: string }) {
  return http.post<CommandLog>(api.care.command, data)
}

export function queryCommandLogs(params: Partial<CommandLogQueryParams>) {
  return http.getPage<CommandLog>(api.care.command, params)
}

