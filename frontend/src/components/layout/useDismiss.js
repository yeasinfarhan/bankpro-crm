import { useEffect } from 'react'

/** Closes a popover when the user clicks outside it or presses Escape. */
export default function useDismiss(ref, open, onClose) {
  useEffect(() => {
    if (!open) return undefined
    const onClick = (e) => ref.current && !ref.current.contains(e.target) && onClose()
    const onKey = (e) => e.key === 'Escape' && onClose()
    document.addEventListener('mousedown', onClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [ref, open, onClose])
}
