import { useCallback, useMemo, useState } from 'react'
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react'
import { ToastContext } from './ToastContext'

const ICONS = { success: CheckCircle2, error: AlertCircle, info: Info }
const TONES = {
  success: 'border-success-600/20 bg-success-50 text-success-600',
  error: 'border-danger-600/20 bg-danger-50 text-danger-600',
  info: 'border-royal-600/20 bg-royal-50 text-royal-700',
}

/** Small toast notifications in the bottom-right corner; they disappear after 4 seconds. */
export default function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const dismiss = useCallback((id) => setToasts((list) => list.filter((t) => t.id !== id)), [])

  const notify = useCallback(
    (message, tone = 'success') => {
      const id = Math.random().toString(36).slice(2)
      setToasts((list) => [...list.slice(-3), { id, message, tone }])
      setTimeout(() => dismiss(id), 4000)
    },
    [dismiss],
  )

  const value = useMemo(() => ({ notify }), [notify])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed bottom-4 right-4 z-50 flex w-full max-w-sm flex-col gap-2 px-4 sm:px-0">
        {toasts.map((t) => {
          const Icon = ICONS[t.tone] ?? Info
          return (
            <div key={t.id} role="status" className={`pointer-events-auto flex items-start gap-2 rounded-lg border p-3 text-sm shadow-lg ${TONES[t.tone]}`}>
              <Icon className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
              <span className="flex-1 text-ink">{t.message}</span>
              <button type="button" onClick={() => dismiss(t.id)} aria-label="Dismiss" className="text-ink-muted hover:text-ink">
                <X className="h-4 w-4" />
              </button>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}
