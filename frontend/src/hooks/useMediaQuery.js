import { useEffect, useState } from 'react'

const query = (q) => (typeof window !== 'undefined' && typeof window.matchMedia === 'function' ? window.matchMedia(q) : null)

/** True while the CSS media query matches (e.g. '(max-width: 639px)'). False where matchMedia is unavailable. */
export default function useMediaQuery(q) {
  const [matches, setMatches] = useState(() => query(q)?.matches ?? false)
  useEffect(() => {
    const mql = query(q)
    if (!mql) return undefined
    const onChange = () => setMatches(mql.matches)
    onChange()
    mql.addEventListener('change', onChange)
    return () => mql.removeEventListener('change', onChange)
  }, [q])
  return matches
}

export const PHONE = '(max-width: 639px)'
