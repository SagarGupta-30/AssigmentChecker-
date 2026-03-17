export function getApiErrorMessage(err, fallback = 'Something went wrong.') {
  if (!err) return fallback

  if (err.code === 'ERR_NETWORK') {
    return 'Cannot connect to backend at http://localhost:8080. Start backend and try again.'
  }

  const data = err.response?.data

  if (data?.errors && typeof data.errors === 'object') {
    const first = Object.values(data.errors).find(Boolean)
    if (first) return String(first)
  }

  if (typeof data?.message === 'string' && data.message.trim()) {
    return data.message
  }

  if (typeof err.message === 'string' && err.message.trim()) {
    return err.message
  }

  return fallback
}
