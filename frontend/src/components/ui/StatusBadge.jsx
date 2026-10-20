const TONES = {
  success: 'bg-success-50 text-success-600 ring-success-600/20',
  warning: 'bg-warning-50 text-warning-600 ring-warning-600/20',
  danger: 'bg-danger-50 text-danger-600 ring-danger-600/20',
  info: 'bg-royal-50 text-royal-700 ring-royal-600/20',
  neutral: 'bg-gray-100 text-gray-700 ring-gray-500/20',
}

/** Small coloured pill for statuses (lead stage, customer status, connection state ...). */
export default function StatusBadge({ tone = 'neutral', children }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${TONES[tone] ?? TONES.neutral}`}
    >
      {children}
    </span>
  )
}
