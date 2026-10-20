/**
 * Horizontally scrollable area (wide tables, the lead board). It is focusable and labelled so keyboard
 * users can scroll it with the arrow keys (WCAG 2.1.1).
 */
export default function ScrollRegion({ label, className = '', children, as: As = 'div', ...props }) {
  return (
    <As role={As === 'div' ? 'region' : undefined} aria-label={label} tabIndex={0} className={`overflow-x-auto focus-visible:outline-offset-[-2px] ${className}`} {...props}>
      {children}
    </As>
  )
}
