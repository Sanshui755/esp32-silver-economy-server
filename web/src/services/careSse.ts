import { API_BASE_URL } from './request'
import api from './api'
import type { CareEventPush } from '@/types/care'

/**
 * 关怀事件 SSE 客户端
 * EventSource 无法带 Authorization 头，依赖 Sa-Token 的 Cookie（前后端同源代理）；
 * 断线自动重连（浏览器原生行为），收到 care-event 事件回调给订阅方。
 */
let source: EventSource | null = null
let heartbeatTimer: number | null = null

export function openCareSse(onEvent: (push: CareEventPush) => void, onError?: (e: Event) => void) {
  closeCareSse()
  // EventSource 只支持同源/绝对 URL；API_BASE_URL 为空 = 开发环境走 vite 代理
  const url = `${API_BASE_URL}/api${api.care.sse}`
  source = new EventSource(url)

  source.addEventListener('care-event', (e: MessageEvent) => {
    try {
      onEvent(JSON.parse(e.data) as CareEventPush)
    } catch {
      // 非 JSON 数据忽略
    }
  })

  source.onerror = (e) => {
    onError?.(e)
    // EventSource 断开后浏览器会自动重连；连接被服务端关闭(readyState=CLOSED)时手动重连
    if (source && source.readyState === EventSource.CLOSED) {
      stopHeartbeat()
      window.setTimeout(() => {
        // 仅在调用方未主动关闭时重连
        if (source === null) return
        openCareSse(onEvent, onError)
      }, 5000)
    }
  }
}

export function closeCareSse() {
  stopHeartbeat()
  if (source) {
    source.close()
    source = null
  }
}

function stopHeartbeat() {
  if (heartbeatTimer !== null) {
    window.clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}
