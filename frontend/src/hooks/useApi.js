import { useCallback, useEffect, useState } from 'react'
import { getErrorMessage } from '../api/client'

/**
 * Loads data with loading / error / data states.
 * `deps` re-run the request; `reload()` re-runs it manually. Late responses from old requests are ignored.
 *
 * The result is tagged with the deps it was loaded for: as soon as the deps change, the hook reports
 * "loading" instead of handing back data that belongs to the previous inputs (e.g. board data while the
 * table view is loading).
 */
export default function useApi(loader, deps) {
  const [attempt, setAttempt] = useState(0)
  const key = JSON.stringify([...deps, attempt])
  const [state, setState] = useState({ key: null, status: 'loading', data: null, error: null })

  useEffect(() => {
    let active = true
    loader().then(
      (data) => active && setState({ key, status: 'ok', data, error: null }),
      (error) => active && setState({ key, status: 'error', data: null, error: getErrorMessage(error) }),
    )
    return () => {
      active = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])

  const reload = useCallback(() => setAttempt((n) => n + 1), [])
  const setData = useCallback((data) => setState((s) => ({ ...s, status: 'ok', data, error: null })), [])

  const current = state.key === key
  return {
    status: current ? state.status : 'loading',
    data: current ? state.data : null,
    error: current ? state.error : null,
    reload,
    setData,
  }
}
