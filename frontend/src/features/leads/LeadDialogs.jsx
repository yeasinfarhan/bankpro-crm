import { useEffect, useState } from 'react'
import Modal from '../../components/ui/Modal'
import Button from '../../components/ui/Button'
import FormField, { inputClass } from '../../components/ui/FormField'
import { assignLead, changeLeadStatus, convertLead } from '../../api/leadsApi'
import { fetchAssignableStaff } from '../../api/usersApi'
import { getErrorMessage, getFieldErrors } from '../../api/client'
import { LOSS_REASONS } from '../../utils/labels'
import { formatBdt } from '../../utils/format'

/** Mark a lead as lost — the reason is required (backend enforces it too). */
export function LossDialog({ lead, onClose, onDone }) {
  const [reason, setReason] = useState('')
  const [note, setNote] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    if (!reason) {
      setError('Choose why the lead was lost.')
      return
    }
    setBusy(true)
    try {
      onDone(await changeLeadStatus(lead.id, { status: 'LOST', lossReason: reason, lossNote: note || null }), 'Lead marked as lost')
    } catch (e) {
      setError(getErrorMessage(e))
      setBusy(false)
    }
  }

  return (
    <Modal open title={`Mark ${lead.code} as lost`} onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button variant="danger" loading={busy} onClick={submit}>Mark as lost</Button></>}>
      <div className="space-y-4">
        {error && <p role="alert" className="rounded-lg bg-danger-50 p-2.5 text-sm text-danger-600">{error}</p>}
        <FormField label="Reason" id="lossReason" required>
          <select id="lossReason" value={reason} onChange={(e) => setReason(e.target.value)} className={inputClass}>
            <option value="">Select a reason</option>
            {Object.entries(LOSS_REASONS).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </FormField>
        <FormField label="Note" id="lossNote" hint="Optional, up to 500 characters">
          <textarea id="lossNote" rows={2} maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} className={inputClass} />
        </FormField>
      </div>
    </Modal>
  )
}

/**
 * Convert a qualified lead into a SIMULATED sale. Loans and cards become "pending approval" until a manager
 * records the simulated decision. The button is disabled while sending, and the backend is idempotent anyway.
 */
export function ConvertDialog({ lead, onClose, onDone }) {
  const [amount, setAmount] = useState(lead.estimatedValue > 0 ? String(lead.estimatedValue) : '')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const needsApproval = ['LOAN', 'CARD'].includes(lead.productCategory)

  const submit = async () => {
    const value = Number(amount)
    if (!(value > 0)) {
      setError('Enter an amount greater than zero.')
      return
    }
    setBusy(true)
    try {
      const result = await convertLead(lead.id, value)
      onDone(result.lead, result.alreadyConverted ? 'This lead was already converted' : needsApproval
        ? 'Application recorded — waiting for manager approval (simulated)' : 'Lead converted to a simulated sale')
    } catch (e) {
      setError(getFieldErrors(e).amount ?? getErrorMessage(e))
      setBusy(false)
    }
  }

  return (
    <Modal open title={`Convert ${lead.code}`} onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={submit}>Convert to sale</Button></>}>
      <div className="space-y-4 text-sm">
        <p className="text-ink-muted">
          <span className="font-medium text-ink">{lead.product.name}</span> for <span className="font-medium text-ink">{lead.customer.name}</span>.
          Estimated value: {formatBdt(lead.estimatedValue)}.
        </p>
        {error && <p role="alert" className="rounded-lg bg-danger-50 p-2.5 text-danger-600">{error}</p>}
        <FormField label="Sale amount (BDT)" id="amount" required hint="Opening/deposit amount, or loan/card limit applied for.">
          <input id="amount" type="number" min="0.01" step="0.01" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} className={inputClass} />
        </FormField>
        <p className={`rounded-lg p-2.5 text-xs ${needsApproval ? 'bg-warning-50 text-warning-600' : 'bg-royal-50 text-royal-700'}`}>
          {needsApproval
            ? 'Loan and card conversions are recorded as applications pending a (simulated) manager approval. They do not count as completed sales until approved.'
            : 'This records a simulated completed sale. No real account is opened and no money moves.'}
        </p>
      </div>
    </Modal>
  )
}

/** Managers reassign a lead to an officer of their branch; the officer gets an in-app notification. */
export function AssignDialog({ lead, onClose, onDone }) {
  const [staff, setStaff] = useState([])
  const [officerId, setOfficerId] = useState(String(lead.assignedOfficer?.id ?? ''))
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let active = true
    fetchAssignableStaff().then((s) => active && setStaff(s), () => active && setError('Could not load officers.'))
    return () => {
      active = false
    }
  }, [])

  const submit = async () => {
    setBusy(true)
    try {
      onDone(await assignLead(lead.id, Number(officerId)), 'Lead reassigned')
    } catch (e) {
      setError(getErrorMessage(e))
      setBusy(false)
    }
  }

  return (
    <Modal open title={`Assign ${lead.code}`} onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={busy} disabled={!officerId} onClick={submit}>Assign</Button></>}>
      {error && <p role="alert" className="mb-3 rounded-lg bg-danger-50 p-2.5 text-sm text-danger-600">{error}</p>}
      <FormField label="Relationship officer" id="officerId">
        <select id="officerId" value={officerId} onChange={(e) => setOfficerId(e.target.value)} className={inputClass}>
          {staff.map((s) => <option key={s.id} value={s.id}>{s.fullName} — {s.jobTitle}</option>)}
        </select>
      </FormField>
    </Modal>
  )
}
