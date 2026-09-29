import type { PageQueryParams } from './api'

/**
 * 老年关怀模块类型定义
 * 字段与后端 Resp（com.xiaozhi.care.model / common.model.care）一一对应
 */

export interface Binding {
  id: number
  elderId: string
  familyId: string
  relation?: string
  isPrimary?: string
  state?: string
  createTime?: string
  userId?: number
  username?: string
  nickname?: string
}

export interface Elder {
  elderId: string
  name: string
  gender?: string
  birthday?: string
  phone?: string
  address?: string
  avatar?: string
  deviceId?: string
  healthNote?: string
  state?: string
  createTime?: string
  deviceOnline?: string
  deviceBattery?: number
  bindings?: Binding[]
}

export interface CareEvent {
  id: number
  msgId: string
  eventType: string
  elderId: string
  elderName?: string
  severity: 'info' | 'warning' | 'critical'
  source?: string
  payload?: string
  status: string
  handledBy?: number
  handledTime?: string
  createTime?: string
}

export interface MedicationPlan {
  id: number
  planId: string
  elderId: string
  elderName?: string
  medicine: string
  dosage?: string
  takeTime: string
  repeatRule?: 'daily' | 'weekdays' | 'custom'
  weekdays?: string
  voicePrompt?: string
  state?: string
  createdBy?: number
  createTime?: string
  updateTime?: string
}

export interface MedicationRecord {
  id: number
  planId: string
  elderId: string
  elderName?: string
  medicine?: string
  plannedTime: string
  status: 'pending' | 'taken' | 'missed'
  takenTime?: string
  confirmSource?: string
  createTime?: string
}

export interface CareDevice {
  deviceId: string
  elderId?: string
  elderName?: string
  online: string
  battery?: number
  rssi?: number
  ip?: string
  firmware?: string
  lastHeartbeat?: string
  lastOnlineTime?: string
  updateTime?: string
}

export interface CommandLog {
  id: number
  msgId: string
  commandType: string
  elderId: string
  elderName?: string
  familyId?: string
  operatorId?: number
  payload?: string
  status: 'sent' | 'delivered' | 'acked' | 'failed'
  ackTime?: string
  ackPayload?: string
  createTime?: string
}

// ---------------- 查询参数 ----------------

export interface ElderQueryParams extends PageQueryParams {
  name?: string
  phone?: string
  elderId?: string
}

export interface CareEventQueryParams extends PageQueryParams {
  elderId?: string
  eventType?: string
  severity?: string
  status?: string
}

export interface MedicationRecordQueryParams extends PageQueryParams {
  elderId?: string
  planId?: string
  status?: string
}

export interface CareDeviceQueryParams extends PageQueryParams {
  elderId?: string
  online?: string
}

export interface CommandLogQueryParams extends PageQueryParams {
  elderId?: string
  commandType?: string
  status?: string
}

/** SSE 实时推送的事件报文（后端 CarePushService） */
export interface CareEventPush {
  eventId: number
  eventType: string
  severity: string
  elderId: string
  elderName: string
  payload: string
  createTime: string
}
