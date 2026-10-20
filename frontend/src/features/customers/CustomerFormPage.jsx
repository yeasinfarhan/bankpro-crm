import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import FormField, { inputClass } from '../../components/ui/FormField'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import { createCustomer, fetchCustomer, updateCustomer } from '../../api/customersApi'
import { fetchProducts } from '../../api/productsApi'
import { fetchAssignableStaff } from '../../api/usersApi'
import { getErrorMessage, getFieldErrors } from '../../api/client'
import { useToast } from '../../components/feedback/ToastContext'
import { useAuth } from '../auth/AuthContext'
import { CUSTOMER_STATUSES, EMPLOYMENT_TYPES, INCOME_RANGES, PRODUCT_CATEGORIES, SEGMENTS } from '../../utils/labels'

const BD_MOBILE = /^(?:\+?880|0)1[3-9]\d{8}$/
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const EMPTY = {
  fullName: '', phone: '', email: '', occupation: '', employmentType: '', incomeRange: '', city: '',
  segment: 'MASS', status: 'PROSPECT', assignedOfficerId: '', productIds: [],
}

/** Create or edit a customer. The backend re-validates everything and normalises phone/email. */
export default function CustomerFormPage() {
  const { id } = useParams()
  const editing = Boolean(id)
  const { user } = useAuth()
  const isManager = user.role === 'BRANCH_MANAGER'
  const navigate = useNavigate()
  const { notify } = useToast()
  const [load, setLoad] = useState({ status: 'loading', version: null, error: null })
  const [products, setProducts] = useState([])
  const [staff, setStaff] = useState([])
  const [serverError, setServerError] = useState(null)

  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm({ defaultValues: EMPTY })

  useEffect(() => {
    let active = true
    Promise.all([
      fetchProducts(),
      isManager ? fetchAssignableStaff() : Promise.resolve([]),
      editing ? fetchCustomer(id) : Promise.resolve(null),
    ]).then(
      ([productList, staffList, customer]) => {
        if (!active) return
        setProducts(productList)
        setStaff(staffList)
        if (customer) {
          if (!customer.permissions.canEdit) {
            setLoad({ status: 'error', error: 'You do not have permission to edit this customer.' })
            return
          }
          reset({
            ...EMPTY,
            ...Object.fromEntries(Object.entries(customer).map(([k, v]) => [k, v ?? ''])),
            phone: customer.phone.replace(/^\+88/, ''),
            assignedOfficerId: customer.assignedOfficer ? String(customer.assignedOfficer.id) : '',
            productIds: customer.products.filter((p) => !p.fromSale).map((p) => String(p.productId)),
          })
        }
        setLoad({ status: 'ok', version: customer?.version ?? null, error: null })
      },
      (error) => active && setLoad({ status: 'error', error: getErrorMessage(error) }),
    )
    return () => {
      active = false
    }
  }, [editing, id, isManager, reset])

  const onSubmit = async (values) => {
    setServerError(null)
    const body = {
      ...values,
      email: values.email || null,
      employmentType: values.employmentType || null,
      incomeRange: values.incomeRange || null,
      assignedOfficerId: values.assignedOfficerId ? Number(values.assignedOfficerId) : null,
      productIds: (values.productIds || []).map(Number),
      version: load.version,
    }
    try {
      const saved = editing ? await updateCustomer(id, body) : await createCustomer(body)
      notify(editing ? 'Customer updated' : `Customer ${saved.code} created`)
      navigate(`/customers/${saved.id}`)
    } catch (error) {
      Object.entries(getFieldErrors(error)).forEach(([field, message]) => setError(field, { message }))
      setServerError(getErrorMessage(error))
    }
  }

  const title = editing ? 'Edit customer' : 'New customer'
  const field = (name) => ({ 'aria-invalid': errors[name] ? 'true' : 'false', 'aria-describedby': errors[name] ? `${name}-error` : undefined })

  return (
    <>
      <PageHeader title={title} breadcrumbs={[{ label: 'Home', to: '/' }, { label: 'Customers', to: '/customers' }, { label: title }]} />
      <Card className="max-w-4xl p-6">
        {load.status === 'loading' && <LoadingState />}
        {load.status === 'error' && <ErrorState message={load.error} />}
        {load.status === 'ok' && (
          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-8">
            {serverError && <div role="alert" className="rounded-lg bg-danger-50 p-3 text-sm text-danger-600">{serverError}</div>}

            <fieldset className="grid gap-5 sm:grid-cols-2">
              <legend className="mb-3 text-sm font-semibold text-ink">Contact</legend>
              <FormField label="Full name" id="fullName" required error={errors.fullName?.message} className="sm:col-span-2">
                <input id="fullName" className={inputClass} {...field('fullName')}
                  {...register('fullName', { required: 'Full name is required', minLength: { value: 2, message: 'Name must be at least 2 characters' }, maxLength: { value: 120, message: 'Name is too long' } })} />
              </FormField>
              <FormField label="Mobile number" id="phone" required error={errors.phone?.message} hint="Bangladesh mobile, e.g. 01712345678">
                <input id="phone" inputMode="tel" className={inputClass} {...field('phone')}
                  {...register('phone', { required: 'Phone number is required', validate: (v) => BD_MOBILE.test(v.replace(/[\s\-()]/g, '')) || 'Enter a valid Bangladesh mobile number' })} />
              </FormField>
              <FormField label="Email" id="email" error={errors.email?.message} hint="Optional">
                <input id="email" type="email" className={inputClass} {...field('email')}
                  {...register('email', { validate: (v) => !v || EMAIL.test(v.trim()) || 'Enter a valid email address' })} />
              </FormField>
              <FormField label="City" id="city" error={errors.city?.message}>
                <input id="city" className={inputClass} {...register('city', { maxLength: { value: 80, message: 'City is too long' } })} />
              </FormField>
            </fieldset>

            <fieldset className="grid gap-5 sm:grid-cols-2">
              <legend className="mb-3 text-sm font-semibold text-ink">Profile</legend>
              <FormField label="Occupation" id="occupation" error={errors.occupation?.message}>
                <input id="occupation" className={inputClass} {...register('occupation', { maxLength: { value: 80, message: 'Occupation is too long' } })} />
              </FormField>
              <FormField label="Employment type" id="employmentType">
                <select id="employmentType" className={inputClass} {...register('employmentType')}>
                  <option value="">Not specified</option>
                  {Object.entries(EMPLOYMENT_TYPES).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                </select>
              </FormField>
              <FormField label="Income range" id="incomeRange" hint="Self-declared band only">
                <select id="incomeRange" className={inputClass} {...register('incomeRange')}>
                  <option value="">Not specified</option>
                  {Object.entries(INCOME_RANGES).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                </select>
              </FormField>
              <FormField label="Segment" id="segment" required error={errors.segment?.message}>
                <select id="segment" className={inputClass} {...register('segment', { required: 'Segment is required' })}>
                  {Object.entries(SEGMENTS).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                </select>
              </FormField>
              <FormField label="Status" id="status" error={errors.status?.message}>
                <select id="status" className={inputClass} {...register('status')}>
                  {Object.entries(CUSTOMER_STATUSES).filter(([v]) => v !== 'ARCHIVED').map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                </select>
              </FormField>
              {isManager && (
                <FormField label="Relationship officer" id="assignedOfficerId" error={errors.assignedOfficerId?.message}>
                  <select id="assignedOfficerId" className={inputClass} {...register('assignedOfficerId')}>
                    <option value="">Unassigned</option>
                    {staff.map((s) => <option key={s.id} value={s.id}>{s.fullName} — {s.jobTitle}</option>)}
                  </select>
                </FormField>
              )}
            </fieldset>

            <fieldset>
              <legend className="text-sm font-semibold text-ink">Existing banking products</legend>
              <p className="mt-1 text-xs text-ink-muted">Products the customer already holds. Products from converted sales are added automatically.</p>
              {errors.productIds && <p className="mt-1 text-xs text-danger-600">{errors.productIds.message}</p>}
              <div className="mt-3 grid gap-2 sm:grid-cols-2">
                {products.map((p) => (
                  <label key={p.id} className="flex items-center gap-2 rounded-lg border border-line px-3 py-2 text-sm hover:bg-canvas">
                    <input type="checkbox" value={String(p.id)} className="h-4 w-4 rounded border-line" {...register('productIds')} />
                    <span>{p.name}</span>
                    <span className="ml-auto text-xs text-ink-muted">{PRODUCT_CATEGORIES[p.category]}</span>
                  </label>
                ))}
              </div>
            </fieldset>

            <div className="flex justify-end gap-2 border-t border-line pt-5">
              <Button variant="secondary" onClick={() => navigate(editing ? `/customers/${id}` : '/customers')}>Cancel</Button>
              <Button type="submit" loading={isSubmitting}>{editing ? 'Save changes' : 'Create customer'}</Button>
            </div>
          </form>
        )}
      </Card>
    </>
  )
}
