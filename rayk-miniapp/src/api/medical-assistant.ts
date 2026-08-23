import { ApiError, getApiBaseUrl, getRequestHeaders, request } from '@/utils/request'

export type MedicalAssistantRole = 'USER' | 'ASSISTANT'
export type MedicalAssistantRiskLevel = 'INFO' | 'ATTENTION' | 'URGENT'

export interface MedicalAssistantMessage {
  id: string
  role: MedicalAssistantRole
  content: string
  riskLevel?: MedicalAssistantRiskLevel
  emergency: boolean
  recommendedAction?: string
  citations: string[]
  usedContext: string[]
  followupQuestions: string[]
  model?: string
  createdAt: string
}

export interface MedicalAssistantConversation {
  id: string
  title: string
  status: string
  model: string
  createdAt: string
  updatedAt: string
  messages: MedicalAssistantMessage[]
}

const MOJIBAKE_MARKER_PATTERN = /[ÃÂâåæçèéêëìíîïñòóôõöùúûüýþÿ]/
const MOJIBAKE_MARKER_GLOBAL_PATTERN = /[ÃÂâåæçèéêëìíîïñòóôõöùúûüýþÿ]/g
const WINDOWS_1252_EXTENDED = new Map(
  Array.from('€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”•–—˜™š›œžŸ').map((character, index) => [
    character.charCodeAt(0),
    index + 0x80,
  ]),
)

/**
 * Repairs text that was previously decoded as Latin-1/Windows-1252 instead of
 * UTF-8. New responses should arrive with an explicit UTF-8 content type; this
 * compatibility layer also makes already persisted malformed conversations
 * readable without changing their stored source data.
 */
export function normalizeMedicalAssistantText(value: string): string {
  if (!value || !MOJIBAKE_MARKER_PATTERN.test(value)) return value

  let normalized = value
  for (let attempt = 0; attempt < 2; attempt += 1) {
    const repaired = repairMojibakeRuns(normalized)
    if (repaired === normalized) break
    normalized = repaired
  }
  return normalized
}

function repairMojibakeRuns(value: string): string {
  let output = ''
  let run = ''

  const flushRun = () => {
    if (!run) return
    output += repairMojibakeRun(run)
    run = ''
  }

  for (const character of value) {
    const code = character.charCodeAt(0)
    if (code <= 0xff || WINDOWS_1252_EXTENDED.has(code)) {
      run += character
    } else {
      flushRun()
      output += character
    }
  }
  flushRun()
  return output
}

function repairMojibakeRun(value: string): string {
  if (!MOJIBAKE_MARKER_PATTERN.test(value)) return value

  const bytes: number[] = []
  for (const character of value) {
    const code = character.charCodeAt(0)
    const byte = code <= 0xff ? code : WINDOWS_1252_EXTENDED.get(code)
    if (byte == null) return value
    bytes.push(byte)
  }

  try {
    const repaired = decodeUtf8Bytes(bytes)
    const sourceScore = countMojibakeMarkers(value)
    const repairedScore = countMojibakeMarkers(repaired)
    return /[\u3400-\u9fff]/.test(repaired) && repairedScore < sourceScore ? repaired : value
  } catch {
    return value
  }
}

function decodeUtf8Bytes(bytes: number[]): string {
  if (typeof TextDecoder !== 'undefined') {
    return new TextDecoder('utf-8', { fatal: true }).decode(new Uint8Array(bytes))
  }
  const binary = bytes.map((byte) => `%${byte.toString(16).padStart(2, '0')}`).join('')
  return decodeURIComponent(binary)
}

function countMojibakeMarkers(value: string): number {
  return value.match(MOJIBAKE_MARKER_GLOBAL_PATTERN)?.length || 0
}

function normalizeMedicalAssistantMessage(message: MedicalAssistantMessage): MedicalAssistantMessage {
  return {
    ...message,
    content: normalizeMedicalAssistantText(message.content),
    recommendedAction: message.recommendedAction
      ? normalizeMedicalAssistantText(message.recommendedAction)
      : message.recommendedAction,
    citations: (message.citations || []).map(normalizeMedicalAssistantText),
    usedContext: (message.usedContext || []).map(normalizeMedicalAssistantText),
    followupQuestions: (message.followupQuestions || []).map(normalizeMedicalAssistantText),
  }
}

function normalizeMedicalAssistantConversation(
  conversation: MedicalAssistantConversation,
): MedicalAssistantConversation {
  return {
    ...conversation,
    title: normalizeMedicalAssistantText(conversation.title),
    messages: (conversation.messages || []).map(normalizeMedicalAssistantMessage),
  }
}

export const listMedicalAssistantConversations = () =>
  request<MedicalAssistantConversation[]>({
    url: '/api/client/medical-assistant/conversations',
    method: 'GET',
  })
    .then((conversations) => conversations.map(normalizeMedicalAssistantConversation))

export const createMedicalAssistantConversation = (title?: string) =>
  request<MedicalAssistantConversation>({
    url: '/api/client/medical-assistant/conversations',
    method: 'POST',
    data: title ? { title } : {},
  })
    .then(normalizeMedicalAssistantConversation)

export const getMedicalAssistantConversation = (conversationId: string) =>
  request<MedicalAssistantConversation>({
    url: `/api/client/medical-assistant/conversations/${encodeURIComponent(conversationId)}`,
    method: 'GET',
  })
    .then(normalizeMedicalAssistantConversation)

export const deleteMedicalAssistantConversation = (conversationId: string) =>
  request<void>({
    url: `/api/client/medical-assistant/conversations/${encodeURIComponent(conversationId)}`,
    method: 'DELETE',
  })

export const sendMedicalAssistantMessage = (conversationId: string, content: string) =>
  request<MedicalAssistantConversation>({
    url: `/api/client/medical-assistant/conversations/${encodeURIComponent(conversationId)}/messages`,
    method: 'POST',
    data: { content },
  })
    .then(normalizeMedicalAssistantConversation)

export interface MedicalAssistantStreamCallbacks {
  onDelta: (content: string) => void
  onDone: (conversation: MedicalAssistantConversation) => void
  onError: (error: Error) => void
}

interface NativeChunkRequestTask {
  abort: () => void
  onChunkReceived?: (callback: (result: { data: ArrayBuffer | string }) => void) => void
}

interface NativeWechatApi {
  request: (options: Record<string, unknown>) => NativeChunkRequestTask
}

/**
 * Read the assistant SSE endpoint progressively on WeChat and H5.
 * WeChat uses wx.request's chunk callback; H5 uses Fetch ReadableStream.
 */
export function streamMedicalAssistantMessage(
  conversationId: string,
  content: string,
  callbacks: MedicalAssistantStreamCallbacks,
) {
  let settled = false
  let aborted = false

  const fail = (cause: unknown) => {
    if (settled || aborted) return
    settled = true
    callbacks.onError(cause instanceof Error ? cause : new Error('健康助手暂时无法回答'))
  }

  const handlePayload = (payload: string) => {
    if (!payload || settled || aborted) return
    try {
      const event = JSON.parse(payload) as {
        type?: string
        content?: string
        message?: string
        code?: number | string
        conversation?: MedicalAssistantConversation
      }
      if (event.type === 'delta' && event.content) {
        callbacks.onDelta(normalizeMedicalAssistantText(event.content))
        return
      }
      if (event.type === 'done' && event.conversation) {
        settled = true
        callbacks.onDone(normalizeMedicalAssistantConversation(event.conversation))
        return
      }
      if (event.type === 'error') {
        fail(toMedicalAssistantError({ code: event.code, message: event.message }))
      }
    } catch (cause) {
      fail(cause)
    }
  }

  const parser = createSseParser(handlePayload)
  const nativeWechat = (globalThis as typeof globalThis & { wx?: NativeWechatApi }).wx

  if (nativeWechat?.request) {
    let receivedChunk = false
    const decoder = createChunkDecoder()
    const task = nativeWechat.request({
      url: `${getApiBaseUrl()}/api/client/medical-assistant/conversations/${encodeURIComponent(conversationId)}/messages/stream`,
      method: 'POST',
      data: { content },
      header: {
        ...getRequestHeaders(),
        Accept: 'text/event-stream',
        'Content-Type': 'application/json',
      },
      dataType: 'text',
      responseType: 'text',
      enableChunked: true,
      timeout: 150000,
      success: (response: unknown) => {
        const result = response as { statusCode?: number; data?: unknown }
        if (result.statusCode && result.statusCode >= 400) {
          fail(toMedicalAssistantError(result.data))
          return
        }
        if (!receivedChunk && result.data != null) {
          parser.push(decoder(result.data))
          parser.flush()
        }
        if (!settled) fail(new Error('健康助手连接已结束，请重试'))
      },
      fail: (response: unknown) => fail(new Error(getNativeRequestError(response))),
    })
    task.onChunkReceived?.((result) => {
      receivedChunk = true
      parser.push(decoder(result.data))
    })
    return {
      abort: () => {
        aborted = true
        task.abort()
      },
    }
  }

  if (typeof fetch === 'function') {
    const controller = new AbortController()
    void (async () => {
      try {
        const response = await fetch(
          `${getApiBaseUrl()}/api/client/medical-assistant/conversations/${encodeURIComponent(conversationId)}/messages/stream`,
          {
            method: 'POST',
            headers: {
              ...getRequestHeaders(),
              Accept: 'text/event-stream',
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({ content }),
            signal: controller.signal,
          },
        )
        if (!response.ok) {
          throw toMedicalAssistantError(await response.text())
        }
        if (!response.body) {
          parser.push(await response.text())
          parser.flush()
          if (!settled) fail(new Error('健康助手连接已结束，请重试'))
          return
        }
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        while (!aborted) {
          const chunk = await reader.read()
          if (chunk.done) {
            parser.push(decoder.decode())
            parser.flush()
            if (!settled) fail(new Error('健康助手连接已结束，请重试'))
            return
          }
          parser.push(decoder.decode(chunk.value, { stream: true }))
        }
      } catch (cause) {
        if (!aborted) fail(cause)
      }
    })()
    return {
      abort: () => {
        aborted = true
        controller.abort()
      },
    }
  }

  void sendMedicalAssistantMessage(conversationId, content)
    .then((conversation) => {
      if (!aborted && !settled) {
        settled = true
        callbacks.onDone(conversation)
      }
    })
    .catch(fail)
  return { abort: () => { aborted = true } }
}

function createSseParser(onPayload: (payload: string) => void) {
  let buffer = ''
  const consume = (block: string) => {
    const data = block
      .split(/\r?\n/)
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trimStart())
      .join('\n')
    if (data && data !== '[DONE]') onPayload(data)
  }
  return {
    push(chunk: string) {
      buffer += chunk
      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        consume(buffer.slice(0, boundary))
        buffer = buffer.slice(boundary + 2)
        boundary = buffer.indexOf('\n\n')
      }
    },
    flush() {
      if (buffer.trim()) consume(buffer)
      buffer = ''
    },
  }
}

function createChunkDecoder() {
  const decoder = typeof TextDecoder !== 'undefined' ? new TextDecoder('utf-8') : undefined
  return (value: unknown) => {
    if (typeof value === 'string') return value
    if (value instanceof ArrayBuffer) {
      if (decoder) return decoder.decode(value, { stream: true })
      let binary = ''
      const bytes = new Uint8Array(value)
      bytes.forEach((byte) => { binary += String.fromCharCode(byte) })
      try { return decodeURIComponent(escape(binary)) } catch { return binary }
    }
    return String(value ?? '')
  }
}

function getNativeRequestError(value: unknown) {
  if (value && typeof value === 'object' && 'errMsg' in value) {
    return String((value as { errMsg?: unknown }).errMsg || '健康助手暂时无法回答')
  }
  return '健康助手暂时无法回答'
}

function toMedicalAssistantError(value: unknown, fallback = '健康助手暂时无法回答'): Error {
  if (value instanceof ApiError) return value

  let candidate = value
  if (typeof candidate === 'string') {
    const text = candidate.trim()
    if (!text) return new Error(fallback)
    try {
      candidate = JSON.parse(text)
    } catch {
      return new Error(text)
    }
  }

  if (candidate && typeof candidate === 'object') {
    const envelope = candidate as { code?: unknown; message?: unknown; data?: unknown }
    let body: { code?: unknown; message?: unknown } = envelope
    if (envelope.data != null) {
      if (typeof envelope.data === 'string') {
        try {
          const parsed = JSON.parse(envelope.data)
          if (parsed && typeof parsed === 'object') {
            body = parsed as { code?: unknown; message?: unknown }
          }
        } catch {
          // Keep the outer response when the body is not JSON.
        }
      } else if (typeof envelope.data === 'object') {
        body = envelope.data as { code?: unknown; message?: unknown }
      }
    }

    const code = Number(body.code ?? envelope.code)
    const rawMessage = body.message ?? envelope.message
    const message = typeof rawMessage === 'string' && rawMessage.trim() ? rawMessage : fallback
    if (Number.isFinite(code) && code !== 0) return new ApiError(code, message)
    if (message !== fallback) return new Error(message)
  }

  return new Error(fallback)
}
