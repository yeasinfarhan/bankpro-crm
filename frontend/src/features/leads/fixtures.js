export const LEAD = {
  id: 12,
  code: 'LD-000012',
  customer: { id: 7, code: 'CUS-000007', name: 'Rafiqul Islam' },
  product: { id: 3, code: 'FDR-1Y', name: 'BankPro Fixed Deposit (FDR)' },
  productCategory: 'DEPOSIT',
  branch: { id: 1, code: 'DHK-MTJ', name: 'Motijheel Branch', city: 'Dhaka' },
  assignedOfficer: { id: 3, fullName: 'Nusrat Jahan', jobTitle: 'Relationship Officer' },
  source: 'REFERRAL',
  status: 'QUALIFIED',
  priority: 'HIGH',
  estimatedValue: 500000,
  expectedCloseDate: '2026-11-30',
  lossReason: null,
  lossNote: null,
  notes: null,
  lastContactedAt: '2026-10-15T04:00:00Z',
  statusChangedAt: '2026-10-16T04:00:00Z',
  createdAt: '2026-10-01T04:00:00Z',
  updatedAt: '2026-10-16T04:00:00Z',
  version: 3,
  allowedStatuses: ['CONTACTED', 'PROPOSAL', 'LOST'],
  permissions: { canEdit: true, canAssign: false, canConvert: true },
  sale: null,
}

export function board(leads = [LEAD]) {
  const statuses = ['NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'CONVERTED', 'LOST']
  return Object.fromEntries(statuses.map((s) => {
    const items = leads.filter((l) => l.status === s)
    return [s, { count: items.length, estimatedValue: items.reduce((a, l) => a + l.estimatedValue, 0), leads: items }]
  }))
}
