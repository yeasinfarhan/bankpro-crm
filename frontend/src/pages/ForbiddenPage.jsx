import { Link } from 'react-router'
import { ShieldAlert } from 'lucide-react'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'

export default function ForbiddenPage() {
  return (
    <Card>
      <EmptyState
        icon={ShieldAlert}
        title="You don't have access to this page"
        description="Your role does not include this area of BankPro. Contact your branch manager if you need access."
        action={<Link to="/" className="text-sm font-medium text-royal-600 hover:underline">Back to dashboard</Link>}
      />
    </Card>
  )
}
