/** Human-readable labels for backend enum values. */
export const SEGMENTS = { MASS: 'Mass', EMERGING_AFFLUENT: 'Emerging affluent', AFFLUENT: 'Affluent', SME: 'SME' }
export const CUSTOMER_STATUSES = { PROSPECT: 'Prospect', ACTIVE: 'Active', INACTIVE: 'Inactive', ARCHIVED: 'Archived' }
export const EMPLOYMENT_TYPES = {
  SALARIED: 'Salaried',
  SELF_EMPLOYED: 'Self-employed',
  BUSINESS_OWNER: 'Business owner',
  PROFESSIONAL: 'Professional',
  STUDENT: 'Student',
  RETIRED: 'Retired',
  OTHER: 'Other',
}
export const INCOME_RANGES = {
  BELOW_30K: 'Below ৳30k / month',
  FROM_30K_TO_60K: '৳30k – 60k / month',
  FROM_60K_TO_100K: '৳60k – 100k / month',
  FROM_100K_TO_200K: '৳100k – 200k / month',
  ABOVE_200K: 'Above ৳200k / month',
}
export const PRODUCT_CATEGORIES = { ACCOUNT: 'Account', DEPOSIT: 'Deposit', CARD: 'Card', LOAN: 'Loan', SME: 'SME' }

export const CUSTOMER_STATUS_TONE = { PROSPECT: 'info', ACTIVE: 'success', INACTIVE: 'warning', ARCHIVED: 'neutral' }

export const options = (map) => Object.entries(map).map(([value, label]) => ({ value, label }))

export const LEAD_STATUSES = {
  NEW: 'New',
  CONTACTED: 'Contacted',
  QUALIFIED: 'Qualified',
  PROPOSAL: 'Proposal',
  CONVERTED: 'Converted',
  LOST: 'Lost',
}
export const LEAD_STATUS_TONE = { NEW: 'info', CONTACTED: 'info', QUALIFIED: 'warning', PROPOSAL: 'warning', CONVERTED: 'success', LOST: 'neutral' }
export const PIPELINE = ['NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'CONVERTED', 'LOST']
export const LEAD_SOURCES = {
  BRANCH_WALK_IN: 'Branch walk-in',
  REFERRAL: 'Referral',
  PHONE_CALL: 'Phone call',
  WEBSITE: 'Website',
  CAMPAIGN: 'Campaign',
  EXISTING_CUSTOMER: 'Existing customer',
}
export const PRIORITIES = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High' }
export const PRIORITY_TONE = { LOW: 'neutral', MEDIUM: 'info', HIGH: 'danger' }
export const LOSS_REASONS = {
  PRICE_OR_RATE: 'Price or rate',
  CHOSE_COMPETITOR: 'Chose a competitor',
  NOT_ELIGIBLE: 'Not eligible',
  NO_RESPONSE: 'No response',
  NO_LONGER_INTERESTED: 'No longer interested',
  OTHER: 'Other',
}
export const SALE_STATUSES = { COMPLETED: 'Completed', PENDING_APPROVAL: 'Pending approval', DECLINED: 'Declined' }
export const SALE_STATUS_TONE = { COMPLETED: 'success', PENDING_APPROVAL: 'warning', DECLINED: 'danger' }
export const ACTIVITY_TYPES = {
  PHONE_CALL: 'Phone call',
  BRANCH_MEETING: 'Branch meeting',
  EMAIL: 'Email',
  PRODUCT_DISCUSSION: 'Product discussion',
  DOCUMENT_COLLECTION: 'Document collection',
  GENERAL_FOLLOW_UP: 'General follow-up',
}
export const INTERACTION_TYPES = { ...ACTIVITY_TYPES, NOTE: 'Note' }
