import { useCallback, useRef, useState } from 'react'
import { ChevronDown } from 'lucide-react'
import useDismiss from '../../components/layout/useDismiss'
import { LEAD_STATUSES } from '../../utils/labels'

/**
 * Keyboard-accessible alternative to drag-and-drop: lists the stages this user may move the lead to,
 * plus "Convert to sale" when allowed. The options come from the API (allowedStatuses / permissions).
 */
export default function StageMenu({ lead, onMove, compact = false, disabled }) {
  const [open, setOpen] = useState(false)
  const ref = useRef(null)
  const close = useCallback(() => setOpen(false), [])
  useDismiss(ref, open, close)

  const options = [
    ...lead.allowedStatuses.filter((s) => s !== 'LOST').map((s) => ({ value: s, label: `Move to ${LEAD_STATUSES[s]}` })),
    ...(lead.permissions.canConvert ? [{ value: 'CONVERTED', label: 'Convert to sale…' }] : []),
    ...(lead.allowedStatuses.includes('LOST') ? [{ value: 'LOST', label: 'Mark as lost…', danger: true }] : []),
  ]
  if (options.length === 0) return null

  return (
    <div className="relative" ref={ref} onClick={(e) => e.stopPropagation()}>
      <button type="button" disabled={disabled} onClick={() => setOpen((v) => !v)} aria-haspopup="menu" aria-expanded={open}
        aria-label={`Change stage of ${lead.code}`}
        className={`inline-flex items-center gap-1 rounded-md border border-line bg-white font-medium text-ink hover:bg-gray-50 disabled:opacity-50 ${compact ? 'px-2 py-1 text-xs' : 'px-3 py-2 text-sm'}`}>
        Move <ChevronDown className="h-3.5 w-3.5" aria-hidden="true" />
      </button>
      {open && (
        <ul role="menu" className="absolute right-0 z-30 mt-1 w-48 rounded-lg border border-line bg-white py-1 shadow-lg">
          {options.map((o) => (
            <li key={o.value} role="none">
              <button type="button" role="menuitem" onClick={() => { close(); onMove(lead, o.value) }}
                className={`block w-full px-3 py-2 text-left text-sm hover:bg-canvas ${o.danger ? 'text-danger-600' : 'text-ink'}`}>
                {o.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
