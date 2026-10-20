import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import { useAuth } from '../auth/AuthContext'
import { ROLE_LABELS } from '../../utils/roles'

export default function SettingsPage() {
  const { user } = useAuth()
  const rows = [
    ['Name', user.fullName],
    ['Email', user.email],
    ['Role', ROLE_LABELS[user.role]],
    ['Job title', user.jobTitle ?? '—'],
    ['Branch', user.branch ? `${user.branch.name} (${user.branch.code})` : 'All branches'],
    ['Business time zone', 'Asia/Dhaka (BST, UTC+6)'],
  ]
  return (
    <>
      <PageHeader title="Settings" breadcrumbs={[{ label: 'Home', to: '/' }, { label: 'Settings' }]} />
      <Card className="max-w-2xl">
        <h2 className="border-b border-line px-6 py-4 text-sm font-semibold">Profile</h2>
        <dl className="divide-y divide-line">
          {rows.map(([label, value]) => (
            <div key={label} className="grid grid-cols-1 gap-1 px-6 py-3 text-sm sm:grid-cols-3">
              <dt className="text-ink-muted">{label}</dt>
              <dd className="font-medium text-ink sm:col-span-2">{value}</dd>
            </div>
          ))}
        </dl>
      </Card>
    </>
  )
}
