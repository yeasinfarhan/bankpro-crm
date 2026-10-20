export const CUSTOMER = {
  id: 7,
  code: 'CUS-000007',
  fullName: 'Rafiqul Islam',
  email: 'rafiqul@example.com',
  phone: '+8801712345678',
  occupation: 'Bank Employee',
  employmentType: 'SALARIED',
  incomeRange: 'FROM_60K_TO_100K',
  city: 'Dhaka',
  segment: 'EMERGING_AFFLUENT',
  status: 'ACTIVE',
  branch: { id: 1, code: 'DHK-MTJ', name: 'Motijheel Branch', city: 'Dhaka' },
  assignedOfficer: { id: 3, fullName: 'Nusrat Jahan', jobTitle: 'Relationship Officer' },
  lastInteractionAt: null,
  createdAt: '2026-05-01T04:00:00Z',
  updatedAt: '2026-05-01T04:00:00Z',
  version: 2,
  products: [{ productId: 1, code: 'SAV-STD', name: 'BankPro Savings Account', category: 'ACCOUNT', since: '2026-05-01', fromSale: false }],
  permissions: { canEdit: true, canArchive: false },
}

export const page = (content) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 })
