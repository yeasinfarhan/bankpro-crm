import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import Modal from '../../components/ui/Modal'
import Button from '../../components/ui/Button'
import FormField, { inputClass } from '../../components/ui/FormField'
import { createTarget, updateTarget } from '../../api/metricsApi'
import { getErrorMessage, getFieldErrors } from '../../api/client'
import { PERIOD_TYPES, TARGET_CATEGORIES } from '../../utils/metricFormat'
import { options } from '../../utils/labels'

/**
 * Create a target (branch manager) or change an existing target's value.
 * Ownership, period and category are fixed after creation — delete and re-create to change them.
 */
export default function TargetFormDialog({ open, target, staff, defaultMonth, onClose, onSaved }) {
  const editing = Boolean(target)
  const [formError, setFormError] = useState(null)
  const { register, handleSubmit, control, setError, formState: { errors, isSubmitting } } = useForm({
    values: editing
      ? { targetValue: String(target.targetValue) }
      : { userId: '', periodType: 'MONTHLY', month: defaultMonth, category: 'DEPOSIT_SALES', targetValue: '' },
  })
  const [watchedCategory, periodType] = useWatch({ control, name: ['category', 'periodType'] })
  const category = editing ? target.category : watchedCategory
  const isMoney = category === 'DEPOSIT_SALES'

  const submit = async (v) => {
    setFormError(null)
    try {
      const saved = editing
        ? await updateTarget(target.id, Number(v.targetValue))
        : await createTarget({
          userId: v.userId ? Number(v.userId) : null,
          periodType: v.periodType,
          periodStart: `${v.month}-01`,
          category: v.category,
          targetValue: Number(v.targetValue),
        })
      onSaved(saved, editing)
    } catch (e) {
      const fields = getFieldErrors(e)
      Object.entries(fields).forEach(([f, m]) => setError(f === 'periodStart' ? 'month' : f, { message: m }))
      if (!Object.keys(fields).length) setFormError(getErrorMessage(e))
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={editing ? 'Edit target' : 'New target'}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancel</Button>
        <Button type="submit" form="target-form" loading={isSubmitting}>{editing ? 'Save' : 'Create target'}</Button>
      </>}>
      <form id="target-form" onSubmit={handleSubmit(submit)} className="space-y-4" noValidate>
        {formError && <p role="alert" className="rounded-lg bg-danger-50 px-3 py-2 text-sm text-danger-600">{formError}</p>}
        {editing ? (
          <p className="text-sm text-ink-muted">
            {TARGET_CATEGORIES[target.category]} · {target.user ? target.user.fullName : 'Whole branch'} · {PERIOD_TYPES[target.periodType]} from {target.periodStart}
          </p>
        ) : (
          <>
            <FormField label="Target for" id="t-user" error={errors.userId?.message}>
              <select id="t-user" className={inputClass} {...register('userId')}>
                <option value="">Whole branch</option>
                {staff.map((u) => <option key={u.id} value={u.id}>{u.fullName}</option>)}
              </select>
            </FormField>
            <div className="grid grid-cols-2 gap-3">
              <FormField label="Period type" id="t-ptype" required>
                <select id="t-ptype" className={inputClass} {...register('periodType')}>
                  {options(PERIOD_TYPES).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select>
              </FormField>
              <FormField label="Month" id="t-month" required error={errors.month?.message}
                hint={periodType === 'QUARTERLY' ? 'Any month in the quarter' : undefined}>
                <input id="t-month" type="month" className={inputClass} aria-invalid={Boolean(errors.month)}
                  {...register('month', { required: 'Choose a month' })} />
              </FormField>
            </div>
            <FormField label="Category" id="t-cat" required>
              <select id="t-cat" className={inputClass} {...register('category')}>
                {options(TARGET_CATEGORIES).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </FormField>
          </>
        )}
        <FormField label={isMoney ? 'Target value (BDT)' : 'Target (count)'} id="t-value" required error={errors.targetValue?.message}
          hint="Fictional planning figure for this demo.">
          <input id="t-value" type="number" inputMode="decimal" min={isMoney ? '0.01' : '1'} step={isMoney ? '0.01' : '1'}
            className={inputClass} aria-invalid={Boolean(errors.targetValue)}
            {...register('targetValue', {
              required: 'Enter a target',
              validate: (val) => {
                const n = Number(val)
                if (!(n > 0)) return 'Target must be greater than zero'
                if (!isMoney && !Number.isInteger(n)) return 'Use a whole number'
                return true
              },
            })} />
        </FormField>
      </form>
    </Modal>
  )
}
