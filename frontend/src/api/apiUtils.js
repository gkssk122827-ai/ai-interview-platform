export function extractPayload(response) {
  return response?.data?.data ?? response?.data ?? null
}

export function cloneData(value) {
  return JSON.parse(JSON.stringify(value))
}

export function delay(ms = 400) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

export function createId(prefix) {
  return `${prefix}-${Math.random().toString(36).slice(2, 10)}`
}
