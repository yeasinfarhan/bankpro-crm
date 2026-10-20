import { useEffect, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import FormField, { inputClass } from '../../components/ui/FormField'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import { createProduct, fetchProduct, updateProduct } from '../../api/productsApi'
import { getErrorMessage, getFieldErrors } from '../../api/client'
import { useToast } from '../../components/feedback/ToastContext'
import { PRODUCT_CATEGORIES } from '../../utils/labels'

const RATE_CATEGORIES = ['ACCOUNT', 'DEPOSIT']
const EMPTY = { code: '', name: '', category: 'ACCOUNT', description: '', eligibilityNotes: '', benefits: '', illustrativeRate: '', active: true }

/** Admin form to add or edit a product. Benefits are entered one per line. */
export default function ProductFormPage() {
  const { id } = useParams()
  const editing = Boolean(id)
  const navigate = useNavigate()
  const { notify } = useToast()
  const [loadState, setLoadState] = useState(editing ? 'loading' : 'ok')
  const [serverError, setServerError] = useState(null)

  const { register, handleSubmit, reset, control, setError, formState: { errors, isSubmitting } } = useForm({ defaultValues: EMPTY })
  const category = useWatch({ control, name: 'category' })

  useEffect(() => {
    if (!editing) return undefined
    let active = true
    fetchProduct(id).then(
      (p) => {
        if (!active) return
        reset({ ...p, benefits: p.benefits.join('\n'), eligibilityNotes: p.eligibilityNotes ?? '', illustrativeRate: p.illustrativeRate ?? '' })
        setLoadState('ok')
      },
      () => active && setLoadState('error'),
    )
    return () => {
      active = false
    }
  }, [editing, id, reset])

  const onSubmit = async (values) => {
    setServerError(null)
    const body = {
      ...values,
      benefits: values.benefits.split('\n').map((b) => b.trim()).filter(Boolean),
      illustrativeRate: RATE_CATEGORIES.includes(values.category) && values.illustrativeRate !== '' ? Number(values.illustrativeRate) : null,
      eligibilityNotes: values.eligibilityNotes || null,
    }
    try {
      if (editing) await updateProduct(id, body)
      else await createProduct(body)
      notify(editing ? 'Product updated' : 'Product created')
      navigate('/products')
    } catch (error) {
      Object.entries(getFieldErrors(error)).forEach(([field, message]) => setError(field, { message }))
      setServerError(getErrorMessage(error))
    }
  }

  const title = editing ? 'Edit product' : 'Add product'
  return (
    <>
      <PageHeader title={title} breadcrumbs={[{ label: 'Home', to: '/' }, { label: 'Banking Products', to: '/products' }, { label: title }]} />
      <Card className="max-w-3xl p-6">
        {loadState === 'loading' && <LoadingState />}
        {loadState === 'error' && <ErrorState message="Product not found." />}
        {loadState === 'ok' && (
          <form onSubmit={handleSubmit(onSubmit)} noValidate className="grid gap-5 sm:grid-cols-2">
            {serverError && <div role="alert" className="rounded-lg bg-danger-50 p-3 text-sm text-danger-600 sm:col-span-2">{serverError}</div>}
            <FormField label="Product code" id="code" required error={errors.code?.message} hint={editing ? 'Codes cannot be changed.' : 'e.g. FDR-2Y'}>
              <input id="code" disabled={editing} aria-invalid={!!errors.code} className={inputClass}
                {...register('code', { required: !editing && 'Code is required', pattern: { value: /^[A-Z0-9][A-Z0-9-]{1,29}$/, message: 'Use 2–30 capital letters, digits or hyphens' } })} />
            </FormField>
            <FormField label="Category" id="category" required error={errors.category?.message}>
              <select id="category" className={inputClass} {...register('category', { required: 'Category is required' })}>
                {Object.entries(PRODUCT_CATEGORIES).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
              </select>
            </FormField>
            <FormField label="Name" id="name" required error={errors.name?.message} className="sm:col-span-2">
              <input id="name" aria-invalid={!!errors.name} className={inputClass} {...register('name', { required: 'Name is required', maxLength: { value: 120, message: 'Name is too long' } })} />
            </FormField>
            <FormField label="Description" id="description" required error={errors.description?.message} className="sm:col-span-2">
              <textarea id="description" rows={3} aria-invalid={!!errors.description} className={inputClass} {...register('description', { required: 'Description is required' })} />
            </FormField>
            <FormField label="Benefits" id="benefits" hint="One per line, up to 8." error={errors.benefits?.message} className="sm:col-span-2">
              <textarea id="benefits" rows={4} className={inputClass} {...register('benefits', { validate: (v) => v.split('\n').filter((b) => b.trim()).length <= 8 || 'At most 8 benefits' })} />
            </FormField>
            <FormField label="Eligibility notes" id="eligibilityNotes" hint="Describe in general terms; BankPro never decides eligibility." className="sm:col-span-2">
              <textarea id="eligibilityNotes" rows={2} className={inputClass} {...register('eligibilityNotes')} />
            </FormField>
            {RATE_CATEGORIES.includes(category) && (
              <FormField label="Illustrative rate (% p.a.)" id="illustrativeRate" hint="Fictional sample value, always labelled as such." error={errors.illustrativeRate?.message}>
                <input id="illustrativeRate" type="number" step="0.01" min="0" max="100" className={inputClass}
                  {...register('illustrativeRate', { min: { value: 0, message: 'Cannot be negative' }, max: { value: 100, message: 'Cannot exceed 100' } })} />
              </FormField>
            )}
            <label className="flex items-center gap-2 self-end pb-2 text-sm text-ink">
              <input type="checkbox" className="h-4 w-4 rounded border-line" {...register('active')} /> Active (visible to staff)
            </label>
            <div className="flex justify-end gap-2 sm:col-span-2">
              <Button variant="secondary" onClick={() => navigate('/products')}>Cancel</Button>
              <Button type="submit" loading={isSubmitting}>{editing ? 'Save changes' : 'Create product'}</Button>
            </div>
          </form>
        )}
      </Card>
    </>
  )
}
