import { useEffect, useRef } from 'react'
import { X } from 'lucide-react'

/** Accessible dialog: focus moves inside, Escape and the backdrop close it. */
export default function Modal({ open, title, onClose, children, footer, size = 'md' }) {
  const panelRef = useRef(null)

  useEffect(() => {
    if (!open) return undefined
    panelRef.current?.querySelector('input, select, textarea, button')?.focus()
    const onKey = (e) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null
  const width = size === 'lg' ? 'max-w-2xl' : 'max-w-md'
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center p-0 sm:items-center sm:p-4">
      <div className="absolute inset-0 bg-navy-950/40" onClick={onClose} aria-hidden="true" />
      <div ref={panelRef} role="dialog" aria-modal="true" aria-labelledby="modal-title"
        className={`relative max-h-[90vh] w-full ${width} overflow-y-auto rounded-t-xl bg-white shadow-xl sm:rounded-xl`}>
        <div className="flex items-center justify-between border-b border-line px-5 py-4">
          <h2 id="modal-title" className="text-base font-semibold text-ink">{title}</h2>
          <button type="button" onClick={onClose} aria-label="Close" className="rounded p-1 text-ink-muted hover:bg-gray-100 hover:text-ink">
            <X className="h-4 w-4" />
          </button>
        </div>
        <div className="px-5 py-4">{children}</div>
        {footer && <div className="flex justify-end gap-2 border-t border-line px-5 py-3">{footer}</div>}
      </div>
    </div>
  )
}
