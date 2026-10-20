import { Link } from 'react-router'
import { SearchX } from 'lucide-react'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'

export default function NotFoundPage() {
  return (
    <Card>
      <EmptyState
        icon={SearchX}
        headingLevel={1}
        title="Page not found"
        description="The page you are looking for does not exist or has moved."
        action={<Link to="/" className="text-sm font-medium text-royal-600 hover:underline">Back to dashboard</Link>}
      />
    </Card>
  )
}
