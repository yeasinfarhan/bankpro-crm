import { useEffect, useState } from 'react'
import useDebounce from './useDebounce'

/**
 * Search box text kept in sync with the `q` URL parameter in both directions:
 * typing updates the URL after a short pause, and a URL change from elsewhere (e.g. the top-bar
 * search, back button) updates the box. `apply(text)` writes the new value to the URL.
 *
 * The URL updates a moment after we ask for it (React Router applies navigations as transitions),
 * so a URL change only counts as "from elsewhere" when it is not the value we pushed ourselves.
 * Otherwise a slow update could overwrite letters typed in the meantime.
 */
export default function useSearchText(params, apply) {
  const urlQ = params.get('q') ?? ''
  const [q, setQ] = useState(urlQ)
  const [prevUrlQ, setPrevUrlQ] = useState(urlQ)
  const [pushed, setPushed] = useState(null)
  if (urlQ !== prevUrlQ) {
    setPrevUrlQ(urlQ)
    if (urlQ !== pushed) setQ(urlQ) // changed by someone else: show the new term
  }
  const debounced = useDebounce(q)

  useEffect(() => {
    if (debounced !== q || debounced === urlQ) return
    setPushed(debounced)
    apply(debounced)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debounced, q, urlQ])

  return [q, setQ]
}
