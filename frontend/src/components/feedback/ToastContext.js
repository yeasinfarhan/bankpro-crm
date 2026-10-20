import { createContext, useContext } from 'react'

export const ToastContext = createContext({ notify: () => {} })

/** const { notify } = useToast(); notify('Saved', 'success') */
export function useToast() {
  return useContext(ToastContext)
}
