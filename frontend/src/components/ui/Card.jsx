/** White panel with border and soft shadow. Extra props (role, aria-*, id ...) are passed to the element. */
export default function Card({ className = '', children, ...props }) {
  return (
    <div {...props} className={`rounded-xl border border-line bg-white shadow-card ${className}`}>
      {children}
    </div>
  )
}
