import { useEffect, useState } from 'react'
import Modal from '../../components/ui/Modal'
import Button from '../../components/ui/Button'
import FormField, { inputClass } from '../../components/ui/FormField'
import CustomerPicker from '../../components/ui/CustomerPicker'
import { completeFollowUp, createFollowUp, rescheduleFollowUp } from '../../api/followUpsApi'
import { fetchLeads } from '../../api/leadsApi'
import { getErrorMessage, getFieldErrors } from '../../api/client'
import { ACTIVITY_TYPES } from '../../utils/labels'
import { bstInputToIso, formatDateTime, isoToBstInput } from '../../utils/format'

/** Default suggestion for a new follow-up: tomorrow 10:00 BST. */
function tomorrowAtTen() {
  const tomorrow = new Date(Date.now() + 6 * 3600 * 1000 + 24 * 3600 * 1000).toISOString().slice(0, 10)
  return `${tomorrow}T10:00`
}

/** Schedule a follow-up. `customer` / `lead` can be pre-filled from the customer or lead page. */
export function ScheduleDialog({ customer: initialCustomer = null, lead = null, onClose, onDone }) {
  const [customer, setCustomer] = useState(initialCustomer)
  const [leadId, setLeadId] = useState(lead ? String(lead.id) : '')
  const [leads, setLeads] = useState([])
  const [type, setType] = useState('PHONE_CALL')
  const [when, setWhen] = useState(tomorrowAtTen())
  const [notes, setNotes] = useState('')
  const [errors, setErrors] = useState({})
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!customer || lead) return undefined
    let active = true
    fetchLeads({ customerId: customer.id, status: ['NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL'], size: 50 })
      .then((p) => active && setLeads(p.content), () => active && setLeads([]))
    return () => {
      active = false
    }
  }, [customer, lead])

  const submit = async () => {
    const local = {}
    if (!customer) local.customerId = 'Choose a customer'
    if (!when) local.scheduledAt = 'Choose a date and time'
    if (Object.keys(local).length) return setErrors(local)
    setBusy(true)
    try {
      const created = await createFollowUp({
        customerId: customer.id,
        leadId: leadId ? Number(leadId) : null,
        activityType: type,
        scheduledAt: bstInputToIso(when),
        notes: notes || null,
      })
      onDone(created, 'Follow-up scheduled')
    } catch (e) {
      setErrors({ ...getFieldErrors(e), form: getErrorMessage(e) })
      setBusy(false)
    }
  }

  return (
    <Modal open title="Schedule follow-up" onClose={onClose} size="lg"
      footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={submit}>Schedule</Button></>}>
      <div className="grid gap-4 sm:grid-cols-2">
        {errors.form && <p role="alert" className="rounded-lg bg-danger-50 p-2.5 text-sm text-danger-600 sm:col-span-2">{errors.form}</p>}
        <FormField label="Customer" id="fu-customer" required error={errors.customerId} className="sm:col-span-2">
          {initialCustomer ? (
            <p className="rounded-lg border border-line bg-canvas px-3 py-2 text-sm">{initialCustomer.name} <span className="text-ink-muted">{initialCustomer.code}</span></p>
          ) : (
            <CustomerPicker id="fu-customer" value={customer} onChange={(c) => { setCustomer(c); setLeadId('') }} invalid={!!errors.customerId} />
          )}
        </FormField>
        <FormField label="Related lead" id="fu-lead" error={errors.leadId} hint="Optional">
          {lead ? (
            <p className="rounded-lg border border-line bg-canvas px-3 py-2 text-sm">{lead.code} · {lead.product.name}</p>
          ) : (
            <select id="fu-lead" value={leadId} onChange={(e) => setLeadId(e.target.value)} className={inputClass} disabled={!customer}>
              <option value="">None</option>
              {leads.map((l) => <option key={l.id} value={l.id}>{l.code} · {l.product.name}</option>)}
            </select>
          )}
        </FormField>
        <FormField label="Activity" id="fu-type" required>
          <select id="fu-type" value={type} onChange={(e) => setType(e.target.value)} className={inputClass}>
            {Object.entries(ACTIVITY_TYPES).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </FormField>
        <FormField label="Date and time (BST)" id="fu-when" required error={errors.scheduledAt}>
          <input id="fu-when" type="datetime-local" value={when} onChange={(e) => setWhen(e.target.value)} className={inputClass} />
        </FormField>
        <FormField label="Notes" id="fu-notes" className="sm:col-span-2">
          <textarea id="fu-notes" rows={2} maxLength={2000} value={notes} onChange={(e) => setNotes(e.target.value)} className={inputClass} />
        </FormField>
      </div>
    </Modal>
  )
}

/** Record the outcome. It becomes part of the customer's interaction history. */
export function CompleteDialog({ followUp, onClose, onDone }) {
  const [outcome, setOutcome] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    if (!outcome.trim()) return setError('Describe the outcome.')
    setBusy(true)
    try {
      onDone(await completeFollowUp(followUp.id, outcome.trim(), notes.trim() || null), 'Follow-up completed')
    } catch (e) {
      setError(getErrorMessage(e))
      setBusy(false)
    }
  }

  return (
    <Modal open title="Complete follow-up" onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={submit}>Mark complete</Button></>}>
      <div className="space-y-4">
        <p className="text-sm text-ink-muted">{ACTIVITY_TYPES[followUp.activityType]} with <span className="font-medium text-ink">{followUp.customer.name}</span> · {formatDateTime(followUp.scheduledAt)}</p>
        {error && <p role="alert" className="rounded-lg bg-danger-50 p-2.5 text-sm text-danger-600">{error}</p>}
        <FormField label="Outcome" id="fu-outcome" required>
          <input id="fu-outcome" maxLength={500} value={outcome} onChange={(e) => setOutcome(e.target.value)} className={inputClass} placeholder="e.g. Customer agreed to visit the branch" />
        </FormField>
        <FormField label="Notes" id="fu-complete-notes">
          <textarea id="fu-complete-notes" rows={3} maxLength={2000} value={notes} onChange={(e) => setNotes(e.target.value)} className={inputClass} />
        </FormField>
      </div>
    </Modal>
  )
}

export function RescheduleDialog({ followUp, onClose, onDone }) {
  const [when, setWhen] = useState(isoToBstInput(followUp.scheduledAt))
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    setBusy(true)
    try {
      onDone(await rescheduleFollowUp(followUp.id, bstInputToIso(when)), 'Follow-up rescheduled')
    } catch (e) {
      setError(getFieldErrors(e).scheduledAt ?? getErrorMessage(e))
      setBusy(false)
    }
  }

  return (
    <Modal open title="Reschedule follow-up" onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={submit}>Save</Button></>}>
      {error && <p role="alert" className="mb-3 rounded-lg bg-danger-50 p-2.5 text-sm text-danger-600">{error}</p>}
      <FormField label="New date and time (BST)" id="fu-reschedule">
        <input id="fu-reschedule" type="datetime-local" value={when} onChange={(e) => setWhen(e.target.value)} className={inputClass} />
      </FormField>
    </Modal>
  )
}
