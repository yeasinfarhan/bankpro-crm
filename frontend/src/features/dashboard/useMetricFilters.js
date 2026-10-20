import { useSearchParams } from 'react-router'
import { presetRange } from '../../utils/period'

/** Period + branch filter kept in the URL so a filtered dashboard/report can be bookmarked or shared. */
export default function useMetricFilters(defaultPreset = 'last6') {
  const [params, setParams] = useSearchParams()
  const preset = params.get('period') ?? defaultPreset
  const fallback = presetRange(preset === 'custom' ? defaultPreset : preset)
  const range = {
    preset,
    from: preset === 'custom' ? params.get('from') ?? fallback.from : fallback.from,
    to: preset === 'custom' ? params.get('to') ?? fallback.to : fallback.to,
  }
  const branchId = params.get('branchId') ?? ''

  const update = (changes) => {
    const next = new URLSearchParams(params)
    for (const [k, v] of Object.entries(changes)) {
      if (v === '' || v === null || v === undefined) next.delete(k)
      else next.set(k, v)
    }
    setParams(next, { replace: true })
  }

  const setRange = (r, extra = {}) =>
    update({ ...extra, ...(r.preset === 'custom' ? { period: 'custom', from: r.from, to: r.to } : { period: r.preset, from: null, to: null }) })

  return { range, branchId, setRange, setBranchId: (id) => update({ branchId: id }), params, update }
}
