import { useEffect, useState } from 'react'
import { CheckCircle2, RefreshCw, ServerCrash } from 'lucide-react'
import Logo from '../../components/ui/Logo'
import StatusBadge from '../../components/ui/StatusBadge'
import { fetchSystemInfo } from '../../api/systemApi'
import { getErrorMessage } from '../../api/client'

/**
 * Phase 2 landing page: proves the React app can talk to the Spring Boot API.
 * Replaced by the login page in Phase 3.
 */
export default function SystemStatusPage() {
  const [state, setState] = useState({ status: 'loading', info: null, error: null })
  const [attempt, setAttempt] = useState(0)

  // Runs on first render and whenever "Check again" bumps `attempt`.
  // `active` ignores a late response if the user left the page or retried meanwhile.
  useEffect(() => {
    let active = true
    fetchSystemInfo().then(
      (info) => active && setState({ status: 'ok', info, error: null }),
      (error) => active && setState({ status: 'error', info: null, error: getErrorMessage(error) }),
    )
    return () => {
      active = false
    }
  }, [attempt])

  const handleRetry = () => {
    setState({ status: 'loading', info: null, error: null })
    setAttempt((n) => n + 1)
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-10">
      <section className="w-full max-w-md rounded-xl border border-line bg-white p-6 shadow-card">
        <Logo />
        <p className="mt-1 text-sm text-ink-muted">Smarter Customer Relationships. Better Banking Sales.</p>

        <div className="mt-6 flex items-center justify-between border-t border-line pt-5">
          <h1 className="text-sm font-semibold text-ink">Backend connection</h1>
          {state.status === 'loading' && <StatusBadge tone="neutral">Checking…</StatusBadge>}
          {state.status === 'ok' && <StatusBadge tone="success">Connected</StatusBadge>}
          {state.status === 'error' && <StatusBadge tone="danger">Unreachable</StatusBadge>}
        </div>

        {state.status === 'loading' && (
          <div className="mt-4 space-y-2" aria-hidden="true">
            <div className="h-4 w-3/4 animate-pulse rounded bg-gray-100" />
            <div className="h-4 w-1/2 animate-pulse rounded bg-gray-100" />
          </div>
        )}

        {state.status === 'ok' && (
          <dl className="mt-4 grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
            <dt className="text-ink-muted">Application</dt>
            <dd className="flex items-center gap-1.5 font-medium">
              <CheckCircle2 className="h-4 w-4 text-success-600" aria-hidden="true" />
              {state.info.application} v{state.info.version}
            </dd>
            <dt className="text-ink-muted">Business time</dt>
            <dd className="font-medium">
              {state.info.businessLocalTime} <span className="text-ink-muted">({state.info.businessZone})</span>
            </dd>
          </dl>
        )}

        {state.status === 'error' && (
          <div role="alert" className="mt-4 rounded-lg bg-danger-50 p-3 text-sm text-danger-600">
            <p className="flex items-center gap-2 font-medium">
              <ServerCrash className="h-4 w-4" aria-hidden="true" />
              {state.error}
            </p>
            <p className="mt-1 text-ink-muted">Start MySQL and the Spring Boot backend, then retry.</p>
          </div>
        )}

        <button
          type="button"
          onClick={handleRetry}
          className="mt-6 inline-flex items-center gap-2 rounded-lg bg-navy-900 px-4 py-2 text-sm font-medium text-white hover:bg-navy-800 disabled:opacity-60"
          disabled={state.status === 'loading'}
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          Check again
        </button>

        <p className="mt-6 text-xs text-ink-muted">
          Portfolio demonstration. Fictional bank and synthetic data only.
        </p>
      </section>
    </main>
  )
}
