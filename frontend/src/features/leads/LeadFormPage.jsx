import { useEffect, useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import FormField, { inputClass } from '../../components/ui/FormField'
import CustomerPicker from '../../components/ui/CustomerPicker'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import { createLead, fetchLead, updateLead } from '../../api/leadsApi'
import { fetchCustomer } from '../../api/customersApi'
import { fetchProducts } from '../../api/productsApi'
import { fetchAssignableStaff } from '../../api/usersApi'
import { getErrorMessage, getFieldErrors } from '../../api/client'
import { useToast } from '../../components/feedback/ToastContext'
import { useAuth } from '../auth/AuthContext'
import { LEAD_SOURCES, PRIORITIES, PRODUCT_CATEGORIES } from '../../utils/labels'

const EMPTY = { customer: null, productId: '', source: 'BRANCH_WALK_IN', priority: 'MEDIUM', estimatedValue: '', expectedCloseDate: '', notes: '', assignedOfficerId: '' }

/** Create (optionally pre-filled with ?customerId=) or edit a lead. Stage changes are separate actions. */
export default function LeadFormPage() {
  const { id } = useParams()
  const [search] = useSearchParams()
  const editing = Boolean(id)
  const { user } = useAuth()
  const isManager = user.role === 'BRANCH_MANAGER'
  const navigate = useNavigate()
  const { notify } = useToast()
  const [load, setLoad] = useState({ status: 'loading', version: null })
  const [products, setProducts] = useState([])
  const [staff, setStaff] = useState([])
  const [serverError, setServerError] = useState(null)
  const { register, control, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm({ defaultValues: EMPTY })

  useEffect(() => {
    let active = true
    const presetCustomer = search.get('customerId')
    Promise.all([
      fetchProducts(),
      isManager ? fetchAssignableStaff() : Promise.resolve([]),
      editing ? fetchLead(id) : Promise.resolve(null),
      !editing && presetCustomer ? fetchCustomer(presetCustomer).catch(() => null) : Promise.resolve(null),
    ]).then(([productList, staffList, lead, customer]) => {
      if (!active) return
      setProducts(productList)
      setStaff(staffList)
      if (lead) {
        if (!lead.permissions.canEdit) return setLoad({ status: 'error', error: 'This lead cannot be edited.' })
        reset({
          customer: lead.customer, productId: String(lead.product.id), source: lead.source, priority: lead.priority,
          estimatedValue: String(lead.estimatedValue), expectedCloseDate: lead.expectedCloseDate ?? '', notes: lead.notes ?? '',
          assignedOfficerId: String(lead.assignedOfficer?.id ?? ''),
        })
      } else if (customer) {
        reset({ ...EMPTY, customer: { id: customer.id, code: customer.code, name: customer.fullName } })
      }
      setLoad({ status: 'ok', version: lead?.version ?? null })
    }, (e) => active && setLoad({ status: 'error', error: getErrorMessage(e) }))
    return () => {
      active = false
    }
  }, [editing, id, isManager, reset, search])

  const onSubmit = async (v) => {
    setServerError(null)
    const body = {
      customerId: v.customer?.id, productId: Number(v.productId), source: v.source, priority: v.priority,
      estimatedValue: Number(v.estimatedValue), expectedCloseDate: v.expectedCloseDate || null, notes: v.notes || null,
      assignedOfficerId: v.assignedOfficerId ? Number(v.assignedOfficerId) : null, version: load.version,
    }
    try {
      const saved = editing ? await updateLead(id, body) : await createLead(body)
      notify(editing ? 'Lead updated' : `Lead ${saved.code} created`)
      navigate(`/leads/${saved.id}`)
    } catch (e) {
      Object.entries(getFieldErrors(e)).forEach(([f, m]) => setError(f === 'customerId' ? 'customer' : f, { message: m }))
      setServerError(getErrorMessage(e))
    }
  }

  const title = editing ? 'Edit lead' : 'New lead'
  return (
    <>
      <PageHeader title={title} breadcrumbs={[{ label: 'Home', to: '/' }, { label: 'Leads', to: '/leads' }, { label: title }]} />
      <Card className="max-w-3xl p-6">
        {load.status === 'loading' && <LoadingState />}
        {load.status === 'error' && <ErrorState message={load.error} />}
        {load.status === 'ok' && (
          <form onSubmit={handleSubmit(onSubmit)} noValidate className="grid gap-5 sm:grid-cols-2">
            {serverError && <div role="alert" className="rounded-lg bg-danger-50 p-3 text-sm text-danger-600 sm:col-span-2">{serverError}</div>}
            <FormField label="Customer" id="customer" required error={errors.customer?.message} className="sm:col-span-2">
              <Controller name="customer" control={control} rules={{ required: 'Choose a customer' }} render={({ field }) => (
                editing
                  ? <p className="rounded-lg border border-line bg-canvas px-3 py-2 text-sm">{field.value?.name} <span className="text-ink-muted">{field.value?.code}</span></p>
                  : <CustomerPicker id="customer" value={field.value} onChange={field.onChange} invalid={!!errors.customer} />
              )} />
            </FormField>
            <FormField label="Interested product" id="productId" required error={errors.productId?.message}>
              <select id="productId" className={inputClass} aria-invalid={!!errors.productId} {...register('productId', { required: 'Choose a product' })}>
                <option value="">Select a product</option>
                {products.map((p) => <option key={p.id} value={p.id}>{p.name} ({PRODUCT_CATEGORIES[p.category]})</option>)}
              </select>
            </FormField>
            <FormField label="Lead source" id="source" required>
              <select id="source" className={inputClass} {...register('source')}>
                {Object.entries(LEAD_SOURCES).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
              </select>
            </FormField>
            <FormField label="Estimated value (BDT)" id="estimatedValue" required error={errors.estimatedValue?.message} hint="Expected opportunity size — not a sale">
              <input id="estimatedValue" type="number" min="0" step="0.01" inputMode="decimal" className={inputClass} aria-invalid={!!errors.estimatedValue}
                {...register('estimatedValue', { required: 'Estimated value is required', min: { value: 0, message: 'Cannot be negative' } })} />
            </FormField>
            <FormField label="Priority" id="priority">
              <select id="priority" className={inputClass} {...register('priority')}>
                {Object.entries(PRIORITIES).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
              </select>
            </FormField>
            <FormField label="Expected closing date" id="expectedCloseDate">
              <input id="expectedCloseDate" type="date" className={inputClass} {...register('expectedCloseDate')} />
            </FormField>
            {isManager && !editing && (
              <FormField label="Assign to" id="assignedOfficerId" error={errors.assignedOfficerId?.message} hint="Leave empty to own it yourself">
                <select id="assignedOfficerId" className={inputClass} {...register('assignedOfficerId')}>
                  <option value="">Me</option>
                  {staff.filter((s) => s.id !== user.id).map((s) => <option key={s.id} value={s.id}>{s.fullName}</option>)}
                </select>
              </FormField>
            )}
            <FormField label="Notes" id="notes" className="sm:col-span-2" error={errors.notes?.message}>
              <textarea id="notes" rows={3} maxLength={2000} className={inputClass} {...register('notes')} />
            </FormField>
            <div className="flex justify-end gap-2 sm:col-span-2">
              <Button variant="secondary" onClick={() => navigate(editing ? `/leads/${id}` : '/leads')}>Cancel</Button>
              <Button type="submit" loading={isSubmitting}>{editing ? 'Save changes' : 'Create lead'}</Button>
            </div>
          </form>
        )}
      </Card>
    </>
  )
}
