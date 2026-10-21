import { useEffect, useState } from 'react'
import { wakeServer } from '../../api/systemApi'

/**
 * Pings the backend as soon as the sign-in page opens. If it has not answered after `slowAfterMs`,
 * the server is probably starting from sleep (free hosting), and the page says so instead of looking broken.
 * Returns 'checking' | 'waking' | 'ready' | 'unreachable'.
 */
export default function useServerWake(slowAfterMs = 2500) {
  const [state, setState] = useState('checking')
  useEffect(() => {
    let active = true
    const slow = setTimeout(() => active && setState((s) => (s === 'checking' ? 'waking' : s)), slowAfterMs)
    wakeServer().then(
      () => active && setState('ready'),
      () => active && setState('unreachable'),
    )
    return () => {
      active = false
      clearTimeout(slow)
    }
  }, [slowAfterMs])
  return state
}
