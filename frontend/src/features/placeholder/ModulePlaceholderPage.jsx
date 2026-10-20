import { Hammer } from 'lucide-react'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import EmptyState from '../../components/ui/EmptyState'

/** Shown for navigation items whose module has not been built yet (tracked in PROJECT_STATUS.md). */
export default function ModulePlaceholderPage({ title, phase }) {
  return (
    <>
      <PageHeader title={title} breadcrumbs={[{ label: 'Home', to: '/' }, { label: title }]} />
      <Card>
        <EmptyState
          icon={Hammer}
          title={`${title} is not built yet`}
          description={`This module is scheduled for ${phase}. Navigation and access rules are already in place.`}
        />
      </Card>
    </>
  )
}
