import { Inbox } from 'lucide-react'

/** Friendly message for "nothing here yet" situations. */
export default function EmptyState({ icon: Icon = Inbox, title, description, action, headingLevel = 2 }) {
  const Heading = `h${headingLevel}`
  return (
    <div className="flex flex-col items-center justify-center px-6 py-12 text-center">
      <span className="flex h-12 w-12 items-center justify-center rounded-full bg-royal-50 text-royal-600">
        <Icon className="h-6 w-6" aria-hidden="true" />
      </span>
      <Heading className={`mt-4 font-semibold text-ink ${headingLevel === 1 ? 'text-base' : 'text-sm'}`}>{title}</Heading>
      {description && <p className="mt-1 max-w-sm text-sm text-ink-muted">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
