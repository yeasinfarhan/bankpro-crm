/**
 * Label + control + error message with the right accessibility wiring.
 * Usage: <FormField label="Email" id="email" error={errors.email?.message}><input id="email" .../></FormField>
 */
export default function FormField({ label, id, error, hint, required, children, className = '' }) {
  return (
    <div className={className}>
      <label htmlFor={id} className="block text-sm font-medium text-ink">
        {label}
        {required && <span className="text-danger-600"> *</span>}
      </label>
      <div className="mt-1">{children}</div>
      {hint && !error && <p className="mt-1 text-xs text-ink-muted">{hint}</p>}
      {error && (
        <p id={`${id}-error`} className="mt-1 text-xs text-danger-600">
          {error}
        </p>
      )}
    </div>
  )
}

/** Shared input styling so every form looks the same. */
export const inputClass =
  'block w-full rounded-lg border border-line bg-white px-3 py-2 text-sm shadow-sm focus:border-royal-500 focus:outline-none focus:ring-2 focus:ring-royal-100 aria-[invalid=true]:border-danger-600 disabled:bg-gray-50'
