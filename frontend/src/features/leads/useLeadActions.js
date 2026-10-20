import { useState } from 'react'
import { changeLeadStatus } from '../../api/leadsApi'
import { getErrorMessage } from '../../api/client'
import { useToast } from '../../components/feedback/ToastContext'

/**
 * Shared "move this lead" logic for the board, table and detail page.
 * LOST and CONVERTED open a dialog (reason / amount); other stages are changed immediately.
 */
export default function useLeadActions(onUpdated) {
  const { notify } = useToast()
  const [dialog, setDialog] = useState(null) // { type: 'lost' | 'convert' | 'assign', lead }
  const [busyId, setBusyId] = useState(null)

  const move = async (lead, status) => {
    if (status === 'LOST') return setDialog({ type: 'lost', lead })
    if (status === 'CONVERTED') return setDialog({ type: 'convert', lead })
    setBusyId(lead.id)
    try {
      onUpdated(await changeLeadStatus(lead.id, { status }))
    } catch (e) {
      notify(getErrorMessage(e), 'error')
    } finally {
      setBusyId(null)
    }
  }

  const done = (updated, message) => {
    setDialog(null)
    notify(message)
    onUpdated(updated)
  }

  return { move, dialog, setDialog, closeDialog: () => setDialog(null), done, busyId }
}
