/** Skeleton rows shown while data loads. `label` is announced to screen readers. */
export default function LoadingState({ rows = 4, label = 'Loading…' }) {
  return (
    <div role="status" aria-live="polite" className="space-y-3 p-4">
      <span className="sr-only">{label}</span>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="h-4 animate-pulse rounded bg-gray-100" style={{ width: `${90 - i * 12}%` }} />
      ))}
    </div>
  )
}
