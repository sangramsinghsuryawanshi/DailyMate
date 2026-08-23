import './PageHeader.css'

/**
 * DailyMate PageHeader Pattern
 * Standardized page header with title, eyebrow/breadcrumb, description, badge, and action slots.
 */
export function PageHeader({
  title,
  eyebrow,
  description,
  badge,
  actions,
  children,
  className = '',
}) {
  return (
    <header className={`dm-page-header ${className}`.trim()}>
      <div className="dm-page-header__main">
        {eyebrow && <div className="dm-page-header__eyebrow">{eyebrow}</div>}
        <div className="dm-page-header__title-row">
          <h1 className="dm-page-header__title">{title}</h1>
          {badge && <div className="dm-page-header__badge">{badge}</div>}
        </div>
        {description && <p className="dm-page-header__description">{description}</p>}
      </div>

      {actions && <div className="dm-page-header__actions">{actions}</div>}
      {children}
    </header>
  )
}

export default PageHeader
