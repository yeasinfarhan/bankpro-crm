/** BankPro wordmark with a simple bank-building mark. */
export default function Logo({ inverted = false }) {
  return (
    <span className="inline-flex items-center gap-2">
      <svg viewBox="0 0 32 32" className="h-8 w-8" aria-hidden="true">
        <rect width="32" height="32" rx="7" fill={inverted ? '#ffffff' : '#0b1f4b'} />
        <path d="M8 13l8-5 8 5v1H8z" fill={inverted ? '#0b1f4b' : '#ffffff'} />
        <path d="M10 15h2.5v7H10zm4.75 0h2.5v7h-2.5zm4.75 0H22v7h-2.5zM8 23h16v2H8z" fill="#3b6ef5" />
      </svg>
      <span className={`text-lg font-semibold tracking-tight ${inverted ? 'text-white' : 'text-navy-900'}`}>
        BankPro
      </span>
    </span>
  )
}
