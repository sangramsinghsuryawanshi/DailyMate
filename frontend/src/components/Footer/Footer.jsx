import { Link } from 'react-router-dom'
import { ResponsiveContainer } from '../../design-system/primitives/ResponsiveContainer/ResponsiveContainer'
import './Footer.css'

export const FOOTER_PRODUCT_LINKS = [
  { label: 'Dashboard', href: '/dashboard' },
  { label: 'AI Assistant', href: '/assistant', badge: '✨ AI' },
  { label: 'Reminders', href: '/medicines' },
  { label: 'Expenses', href: '/expenses' },
  { label: 'Grocery', href: '/grocery' },
  { label: 'Jobs', href: '/jobs' },
  { label: 'Local Services', href: '/marketplace' },
  { label: 'Notifications', href: '/notifications' },
]

export const FOOTER_COMMUNITY_LINKS = [
  { label: 'Blood Donation', href: '/blood', badge: 'Urgent' },
  { label: 'Lost & Found', href: '/lost-found' },
  { label: 'Community Complaints', href: '/community-complaints' },
  { label: 'Local Events', href: '/events' },
  { label: 'Emergency Contacts', href: '/emergency-contacts' },
]

export const FOOTER_COMPANY_LINKS = [
  { label: 'About DailyMate', href: '/about' },
  { label: 'Contact Us', href: '/contact' },
  { label: 'Privacy Policy', href: '/privacy' },
  { label: 'Terms of Service', href: '/terms' },
  { label: 'Help & Support', href: '/faq' },
]

export const FOOTER_SOCIAL_LINKS = [
  {
    name: 'Email',
    href: 'mailto:contact@dailymate.app',
    ariaLabel: 'Send inquiries to DailyMate',
    icon: (
      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
        <path strokeLinecap="round" strokeLinejoin="round" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
      </svg>
    ),
  },
  {
    name: 'LinkedIn',
    href: 'https://linkedin.com',
    ariaLabel: 'Visit DailyMate on LinkedIn',
    isExternal: true,
    icon: (
      <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M19 0h-14c-2.761 0-5 2.239-5 5v14c0 2.761 2.239 5 5 5h14c2.762 0 5-2.239 5-5v-14c0-2.761-2.238-5-5-5zm-11 19h-3v-11h3v11zm-1.5-12.268c-.966 0-1.75-.79-1.75-1.764s.784-1.764 1.75-1.764 1.75.79 1.75 1.764-.783 1.764-1.75 1.764zm13.5 12.268h-3v-5.604c0-3.368-4-3.113-4 0v5.604h-3v-11h3v1.765c1.396-2.586 7-2.777 7 2.476v6.759z" />
      </svg>
    ),
  },
  {
    name: 'GitHub',
    href: 'https://github.com',
    ariaLabel: 'Visit DailyMate on GitHub',
    isExternal: true,
    icon: (
      <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
        <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
      </svg>
    ),
  },
  {
    name: 'Instagram',
    href: 'https://instagram.com',
    ariaLabel: 'Visit DailyMate on Instagram',
    isExternal: true,
    icon: (
      <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z" />
      </svg>
    ),
  },
  {
    name: 'Twitter / X',
    href: 'https://twitter.com',
    ariaLabel: 'Visit DailyMate on X',
    isExternal: true,
    icon: (
      <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
      </svg>
    ),
  },
]

function renderFooterLink(item) {
  const isInternalRoute = item.href.startsWith('/') && !item.href.startsWith('//')
  
  const content = (
    <span className="dm-footer__link-wrapper">
      <span>{item.label}</span>
      {item.badge && (
        <span className="dm-footer__pill-badge dm-footer__pill-badge--accent" aria-hidden="true">
          {item.badge}
        </span>
      )}
    </span>
  )

  if (isInternalRoute) {
    return (
      <Link to={item.href} className="dm-footer__link">
        {content}
      </Link>
    )
  }

  return (
    <a
      href={item.href}
      className="dm-footer__link"
      target={item.isExternal ? '_blank' : undefined}
      rel={item.isExternal ? 'noopener noreferrer' : undefined}
    >
      {content}
    </a>
  )
}

export function Footer({ onCookiePreferencesClick }) {
  const handleScrollTop = () => {
    const mainArea = document.querySelector('.dm-app-shell__main-area')
    if (mainArea && typeof mainArea.scrollTo === 'function') {
      mainArea.scrollTo({ top: 0, behavior: 'smooth' })
    } else if (typeof window.scrollTo === 'function') {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }

  return (
    <footer className="dm-footer" aria-label="DailyMate footer">
      <ResponsiveContainer size="wide">
        <div className="dm-footer__container">
          {/* Main Grid: Multi-Column on Desktop, 2-3 col on Tablet, Stacked on Mobile */}
          <div className="dm-footer__grid">
            {/* Brand Column */}
            <div className="dm-footer__brand-col">
              <div className="dm-footer__brand-row">
                <Link to="/" className="dm-footer__brand-link" aria-label="DailyMate Home">
                  <img
                    src="/images/DailyMateIcon.png"
                    alt="DailyMate Logo"
                    className="dm-footer__logo"
                  />
                  <span>DailyMate</span>
                </Link>
                <span className="dm-footer__version-badge">v2.4 Pro</span>
              </div>

              <p className="dm-footer__tagline">
                Your everyday life, organized.
              </p>

              <p className="dm-footer__description">
                DailyMate is an all-in-one personal life-management platform bringing essential
                everyday tools, routines, finance tracking, local services, community needs, and
                AI assistance together in one unified place.
              </p>

              {/* Social / Contact Row */}
              <div className="dm-footer__social-row" aria-label="DailyMate Social and Contact links">
                {FOOTER_SOCIAL_LINKS.map((s) => (
                  <a
                    key={s.name}
                    href={s.href}
                    className="dm-footer__social-btn"
                    aria-label={s.ariaLabel}
                    title={s.name}
                    target={s.isExternal ? '_blank' : undefined}
                    rel={s.isExternal ? 'noopener noreferrer' : undefined}
                  >
                    {s.icon}
                  </a>
                ))}
              </div>
            </div>

            {/* Product Links */}
            <nav className="dm-footer__nav-col" aria-label="Product Links">
              <h3 className="dm-footer__col-title">
                <span className="dm-footer__col-title-dot" aria-hidden="true" />
                Product
              </h3>
              <ul className="dm-footer__nav-list">
                {FOOTER_PRODUCT_LINKS.map((item) => (
                  <li key={item.label}>
                    {renderFooterLink(item)}
                  </li>
                ))}
              </ul>
            </nav>

            {/* Community Links */}
            <nav className="dm-footer__nav-col" aria-label="Community Links">
              <h3 className="dm-footer__col-title">
                <span className="dm-footer__col-title-dot" aria-hidden="true" />
                Community
              </h3>
              <ul className="dm-footer__nav-list">
                {FOOTER_COMMUNITY_LINKS.map((item) => (
                  <li key={item.label}>
                    {renderFooterLink(item)}
                  </li>
                ))}
              </ul>
            </nav>

            {/* Company Links */}
            <nav className="dm-footer__nav-col" aria-label="Corporate Directory">
              <h3 className="dm-footer__col-title">
                <span className="dm-footer__col-title-dot" aria-hidden="true" />
                Company
              </h3>
              <ul className="dm-footer__nav-list">
                {FOOTER_COMPANY_LINKS.map((item) => (
                  <li key={item.label}>
                    {renderFooterLink(item)}
                  </li>
                ))}
              </ul>
            </nav>
          </div>

          {/* Divider */}
          <div className="dm-footer__divider" role="separator" />

          {/* Bottom Bar */}
          <div className="dm-footer__bottom-bar">
            <p className="dm-footer__copyright">
              © 2026 DailyMate. All rights reserved.
            </p>

            <div className="dm-footer__legal-links" aria-label="Legal & Privacy">
              <Link to="/privacy" className="dm-footer__legal-link">
                Privacy Policy
              </Link>
              <span aria-hidden="true" style={{ opacity: 0.4 }}>·</span>
              <Link to="/terms" className="dm-footer__legal-link">
                Terms of Service
              </Link>
              <span aria-hidden="true" style={{ opacity: 0.4 }}>·</span>
              <button
                type="button"
                className="dm-footer__cookie-btn"
                onClick={onCookiePreferencesClick || (() => window.alert?.('Cookie Preferences: All essential cookies active.'))}
              >
                Cookie Preferences
              </button>

              <button
                type="button"
                className="dm-footer__back-to-top"
                onClick={handleScrollTop}
                title="Back to top of page"
              >
                <span>Top</span>
                <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M5 10l7-7m0 0l7 7m-7-7v18" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </ResponsiveContainer>
    </footer>
  )
}

export default Footer
