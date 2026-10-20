import { AssignDialog, ConvertDialog, LossDialog } from './LeadDialogs'

/** Renders whichever lead dialog is open. */
export default function LeadDialogHost({ dialog, onClose, onDone }) {
  if (!dialog) return null
  const props = { lead: dialog.lead, onClose, onDone }
  if (dialog.type === 'lost') return <LossDialog {...props} />
  if (dialog.type === 'convert') return <ConvertDialog {...props} />
  if (dialog.type === 'assign') return <AssignDialog {...props} />
  return null
}
