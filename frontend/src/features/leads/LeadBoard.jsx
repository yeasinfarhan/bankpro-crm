import { useState } from 'react'
import { Link } from 'react-router'
import StatusBadge from '../../components/ui/StatusBadge'
import StageMenu from './StageMenu'
import { LEAD_STATUSES, PIPELINE, PRIORITIES, PRIORITY_TONE } from '../../utils/labels'
import { formatBdt } from '../../utils/format'

/**
 * Kanban board. Cards can be dragged between columns (mouse) or moved with the "Move" menu (keyboard,
 * touch). A column only accepts a drop if the API says this user may move the lead there.
 */
export default function LeadBoard({ board, onMove, busyId }) {
  const [dragging, setDragging] = useState(null)
  const [over, setOver] = useState(null)

  const canDrop = (status) => {
    if (!dragging || dragging.status === status) return false
    if (status === 'CONVERTED') return dragging.permissions.canConvert
    return dragging.allowedStatuses.includes(status)
  }

  return (
    <div className="flex gap-4 overflow-x-auto pb-4" role="list" aria-label="Lead pipeline">
      {PIPELINE.map((status) => {
        const column = board[status]
        const droppable = canDrop(status)
        return (
          <section
            key={status}
            role="listitem"
            aria-label={`${LEAD_STATUSES[status]}: ${column.count} leads`}
            onDragOver={(e) => { if (droppable) { e.preventDefault(); setOver(status) } }}
            onDragLeave={() => setOver((s) => (s === status ? null : s))}
            onDrop={(e) => {
              e.preventDefault()
              setOver(null)
              if (droppable) onMove(dragging, status)
              setDragging(null)
            }}
            className={`flex w-64 shrink-0 flex-col rounded-xl border bg-canvas transition-colors ${
              over === status ? 'border-royal-500 bg-royal-50' : droppable ? 'border-royal-100' : 'border-line'
            }`}
          >
            <header className="flex items-center justify-between px-3 py-2.5">
              <h2 className="text-sm font-semibold text-ink">
                {LEAD_STATUSES[status]} <span className="font-normal text-ink-muted">({column.count})</span>
              </h2>
              <span className="text-xs text-ink-muted">{formatBdt(column.estimatedValue)}</span>
            </header>
            <ul className="flex min-h-24 flex-col gap-2 px-2 pb-2">
              {column.leads.map((lead) => (
                <li
                  key={lead.id}
                  draggable={lead.allowedStatuses.length > 0 || lead.permissions.canConvert}
                  onDragStart={() => setDragging(lead)}
                  onDragEnd={() => { setDragging(null); setOver(null) }}
                  className={`rounded-lg border border-line bg-white p-3 shadow-card ${busyId === lead.id ? 'opacity-50' : ''}`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <Link to={`/leads/${lead.id}`} className="text-sm font-medium text-ink hover:text-royal-600">
                      {lead.customer.name}
                    </Link>
                    <StatusBadge tone={PRIORITY_TONE[lead.priority]}>{PRIORITIES[lead.priority]}</StatusBadge>
                  </div>
                  <p className="mt-0.5 text-xs text-ink-muted">{lead.product.name}</p>
                  <div className="mt-2 flex items-center justify-between gap-2">
                    <div className="text-xs">
                      <p className="font-medium text-ink">{formatBdt(lead.estimatedValue)}</p>
                      <p className="text-ink-muted">{lead.code} · {lead.assignedOfficer?.fullName?.split(' ')[0]}</p>
                    </div>
                    <StageMenu lead={lead} onMove={onMove} compact disabled={busyId === lead.id} />
                  </div>
                </li>
              ))}
              {column.count > column.leads.length && (
                <li className="px-1 text-xs text-ink-muted">+{column.count - column.leads.length} more — use the table view</li>
              )}
              {column.count === 0 && <li className="px-1 py-4 text-center text-xs text-ink-muted">No leads</li>}
            </ul>
          </section>
        )
      })}
    </div>
  )
}
