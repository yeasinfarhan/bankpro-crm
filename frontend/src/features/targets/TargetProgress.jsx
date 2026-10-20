import { CheckCircle2 } from 'lucide-react'
import { formatMetric, formatPercent, PERIOD_TYPES, TARGET_CATEGORIES, targetOwner } from '../../utils/metricFormat'
import { formatDate } from '../../utils/format'


/** Meter for one target: actual vs target with the % achieved. Met targets get an icon + word, not colour alone. */
export default function TargetProgress({ target, showOwner = true, actions }) {
  const pct = target.achievementPct === null ? null : Number(target.achievementPct)
  const width = pct === null ? 0 : Math.min(pct, 100)
  const met = pct !== null && pct >= 100
  const value = (v) => formatMetric(v, 'unitValue', target)
  return (
    <div className="py-3">
      <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
        <p className="text-sm font-medium text-ink">
          {TARGET_CATEGORIES[target.category]}
          {showOwner && <span className="font-normal text-ink-muted"> · {targetOwner(target)}</span>}
        </p>
        <p className="flex items-center gap-1.5 text-sm tabular-nums text-ink">
          {met && <CheckCircle2 className="h-4 w-4 text-success-600" aria-hidden="true" />}
          <span className="font-semibold">{formatPercent(pct)}</span>
          {met && <span className="text-xs font-medium text-success-600">Met</span>}
          {actions}
        </p>
      </div>
      <div className="mt-2 h-2 overflow-hidden rounded-full bg-gray-100" role="meter" aria-valuemin={0} aria-valuemax={100}
        aria-valuenow={Math.round(width)} aria-label={`${TARGET_CATEGORIES[target.category]} achievement`}>
        <div className="h-full rounded-full bg-[#2a78d6]" style={{ width: `${width}%` }} />
      </div>
      <p className="mt-1.5 text-xs text-ink-muted">
        {value(target.actual)} of {value(target.targetValue)}
        {Number(target.remaining) > 0 && <> · {value(target.remaining)} to go</>}
        {' · '}{PERIOD_TYPES[target.periodType]} {formatDate(target.periodStart)} – {formatDate(target.periodEnd)}
      </p>
    </div>
  )
}
