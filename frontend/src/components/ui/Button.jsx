import { Loader2 } from 'lucide-react'

const VARIANTS = {
  primary: 'bg-navy-900 text-white hover:bg-navy-800 border border-transparent',
  secondary: 'bg-white text-ink border border-line hover:bg-gray-50',
  danger: 'bg-danger-600 text-white hover:bg-red-700 border border-transparent',
  ghost: 'bg-transparent text-ink-muted hover:bg-gray-100 border border-transparent',
}

/** Consistent button. `as` lets a Link look like a button. */
export default function Button({ variant = 'primary', loading = false, icon: Icon, as: As = 'button', className = '', children, ...props }) {
  const typeProps = As === 'button' ? { type: props.type ?? 'button' } : {}
  return (
    <As
      {...props}
      {...typeProps}
      disabled={As === 'button' ? props.disabled || loading : undefined}
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-3.5 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${VARIANTS[variant]} ${className}`}
    >
      {loading ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" /> : Icon && <Icon className="h-4 w-4" aria-hidden="true" />}
      {children}
    </As>
  )
}
