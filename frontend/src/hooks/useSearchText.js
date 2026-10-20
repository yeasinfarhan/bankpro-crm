import { useEffect, useState } from 'react'
import useDebounce from './useDebounce'

/**
 * Search box text kept in sync with the `q` URL parameter in both directions:
 * typing updates the URL after a short pause, and a URL change from elsewhere (e.g. the top-bar
 * search, back button) updates the box. `apply(text)` writes the new value to the URL.
 */
export default function useSearchText(params, apply) {
  const urlQ = params.get('q') ?? ''
  const [q, setQ] = useState(urlQ)
  const [seen, setSeen] = useState(urlQ)
  if (urlQ !== seen) {
    // The URL changed without us typing: show the new term.
    setSeen(urlQ)
    setQ(urlQ)
  }
  const debounced = useDebounce(q)

  useEffect(() => {
    if (debounced !== q || debounced === urlQ) return
    setSeen(debounced)
    apply(debounced)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debounced, q, urlQ])

  return [q, setQ]
}
