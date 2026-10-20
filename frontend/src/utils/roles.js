export const ROLES = {
  ADMIN: 'ADMIN',
  BRANCH_MANAGER: 'BRANCH_MANAGER',
  RELATIONSHIP_OFFICER: 'RELATIONSHIP_OFFICER',
}

export const ROLE_LABELS = {
  ADMIN: 'Administrator',
  BRANCH_MANAGER: 'Branch Manager',
  RELATIONSHIP_OFFICER: 'Relationship Officer',
}

export function hasAnyRole(user, roles) {
  if (!roles || roles.length === 0) return true
  return Boolean(user && roles.includes(user.role))
}

export function initials(name = '') {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('')
}
