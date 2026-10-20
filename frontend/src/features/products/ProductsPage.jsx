import { useState } from 'react'
import { Link } from 'react-router'
import { Landmark, Pencil, Plus } from 'lucide-react'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import SearchBar from '../../components/ui/SearchBar'
import SelectFilter from '../../components/ui/SelectFilter'
import StatusBadge from '../../components/ui/StatusBadge'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import EmptyState from '../../components/ui/EmptyState'
import useApi from '../../hooks/useApi'
import useDebounce from '../../hooks/useDebounce'
import { fetchProducts } from '../../api/productsApi'
import { options, PRODUCT_CATEGORIES } from '../../utils/labels'
import { useAuth } from '../auth/AuthContext'

/** Catalogue of fictional products. Admins also see inactive products and can add/edit. */
export default function ProductsPage() {
  const { user } = useAuth()
  const isAdmin = user.role === 'ADMIN'
  const [q, setQ] = useState('')
  const [category, setCategory] = useState('')
  const [showInactive, setShowInactive] = useState(false)
  const query = useDebounce(q)

  const { status, data, error, reload } = useApi(
    () => fetchProducts({ q: query, category, active: isAdmin && !showInactive ? true : undefined }),
    [query, category, showInactive, isAdmin],
  )

  return (
    <>
      <PageHeader
        title="Banking Products"
        description="Fictional product catalogue. Rates shown are illustrative sample values, not real offers."
        breadcrumbs={[{ label: 'Home', to: '/' }, { label: 'Banking Products' }]}
        actions={isAdmin && <Button as={Link} to="/products/new" icon={Plus}>Add product</Button>}
      />

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end">
        <SearchBar value={q} onChange={setQ} placeholder="Search name or code" label="Search products" />
        <SelectFilter label="Category" value={category} onChange={setCategory} options={options(PRODUCT_CATEGORIES)} />
        {isAdmin && (
          <label className="flex items-center gap-2 pb-2 text-sm text-ink">
            <input type="checkbox" checked={showInactive} onChange={(e) => setShowInactive(e.target.checked)} className="h-4 w-4 rounded border-line" />
            Include inactive
          </label>
        )}
      </div>

      {status === 'loading' && <Card><LoadingState label="Loading products…" /></Card>}
      {status === 'error' && <Card><ErrorState message={error} onRetry={reload} /></Card>}
      {status === 'ok' && data.length === 0 && (
        <Card><EmptyState icon={Landmark} title="No products found" description="Try a different search or category." /></Card>
      )}
      {status === 'ok' && data.length > 0 && (
        <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {data.map((p) => (
            <li key={p.id}>
              <Card className="flex h-full flex-col p-5">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="text-xs font-medium uppercase tracking-wide text-royal-600">{PRODUCT_CATEGORIES[p.category]}</p>
                    <h2 className="mt-1 text-base font-semibold text-ink">{p.name}</h2>
                    <p className="text-xs text-ink-muted">{p.code}</p>
                  </div>
                  {!p.active && <StatusBadge tone="neutral">Inactive</StatusBadge>}
                </div>
                <p className="mt-3 text-sm text-ink-muted">{p.description}</p>
                {p.benefits.length > 0 && (
                  <ul className="mt-3 space-y-1 text-sm text-ink">
                    {p.benefits.map((b) => (
                      <li key={b} className="flex gap-2"><span className="text-royal-600" aria-hidden="true">•</span>{b}</li>
                    ))}
                  </ul>
                )}
                {p.illustrativeRate !== null && (
                  <p className="mt-3 rounded-md bg-warning-50 px-2.5 py-1.5 text-xs text-warning-600">
                    Illustrative rate: <strong>{Number(p.illustrativeRate).toFixed(2)}% p.a.</strong> — fictional sample, not an offer
                  </p>
                )}
                {p.eligibilityNotes && <p className="mt-3 text-xs text-ink-muted">{p.eligibilityNotes}</p>}
                {isAdmin && (
                  <div className="mt-auto pt-4">
                    <Button as={Link} to={`/products/${p.id}/edit`} variant="secondary" icon={Pencil}>Edit</Button>
                  </div>
                )}
              </Card>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
