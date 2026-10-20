import { ChevronLeft, ChevronRight } from 'lucide-react'

/** Server-side pagination footer. `page` is zero-based like the API. */
export default function Pagination({ page, totalPages, totalElements, size, onPageChange }) {
  if (!totalElements) return null
  const from = page * size + 1
  const to = Math.min((page + 1) * size, totalElements)
  return (
    <nav aria-label="Pagination" className="flex items-center justify-between gap-3 border-t border-line px-4 py-3 text-sm">
      <p className="text-ink-muted">
        Showing <span className="font-medium text-ink">{from}</span>–<span className="font-medium text-ink">{to}</span> of{' '}
        <span className="font-medium text-ink">{totalElements}</span>
      </p>
      <div className="flex items-center gap-1">
        <button
          type="button"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0}
          className="inline-flex items-center gap-1 rounded-lg border border-line px-2.5 py-1.5 disabled:opacity-40"
          aria-label="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        <span className="px-2 text-ink-muted">
          Page {page + 1} of {Math.max(totalPages, 1)}
        </span>
        <button
          type="button"
          onClick={() => onPageChange(page + 1)}
          disabled={page + 1 >= totalPages}
          className="inline-flex items-center gap-1 rounded-lg border border-line px-2.5 py-1.5 disabled:opacity-40"
          aria-label="Next page"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </nav>
  )
}
