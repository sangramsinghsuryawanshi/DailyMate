import { Link } from 'react-router-dom'
import { Button, ResponsiveContainer } from '../../design-system'
import PublicLayout from '../../layouts/PublicLayout'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'

function useSafeAuth() {
  try {
    return useAuth()
  } catch {
    return { user: null }
  }
}

const TIERS = [
  {
    name: 'Free Starter',
    price: '$0',
    frequency: 'forever',
    desc: 'Essential personal life management for individuals and families.',
    highlight: false,
    badge: 'Popular',
    features: [
      'Personal medication schedules & adherence tracking',
      'Monthly expense logging & spending breakdown',
      'Access to local services marketplace (100% free contact)',
      'Community blood appeals & lost & found board',
      'Personal emergency ICE contacts with one-tap dial',
      'Standard AI Assistant support (15 actions/month)',
    ],
    cta: 'Get Started Free',
    ctaLink: '/register',
  },
  {
    name: 'DailyMate Pro',
    price: '$8',
    frequency: 'per month',
    desc: 'Advanced automations, smart AI assistants, and multi-profile family support.',
    highlight: true,
    badge: 'Recommended',
    features: [
      'Everything in Free Starter',
      'Unlimited intelligent AI Action Proposals',
      'Multi-profile household health & medication tracking',
      'Custom budget categories & financial export (CSV/PDF)',
      'Priority civic complaints & event organizing tools',
      'Real-time automated SMS alert dispatch for ICE contacts',
      'Early access to new features & 24/7 dedicated support',
    ],
    cta: 'Start 14-Day Free Trial',
    ctaLink: '/register',
  },
]

export default function PricingPage() {
  const { user } = useSafeAuth()
  const Layout = user ? MainLayout : PublicLayout

  return (
    <Layout>
      <div style={{ padding: '3rem 0 4rem 0' }}>
        <ResponsiveContainer size="wide">
          <div style={{ textAlign: 'center', maxWidth: '760px', margin: '0 auto 3.5rem auto' }}>
            <span className="dm-section-eyebrow">Transparent Pricing</span>
            <h1 className="dm-page-main-title" style={{ fontSize: 'clamp(2rem, 4vw, 3rem)' }}>
              Simple, transparent plans for your everyday life
            </h1>
            <p className="dm-page-subtitle" style={{ fontSize: '1.125rem' }}>
              Start for free with essential tools, or upgrade to Pro for advanced intelligence and family management.
            </p>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
              gap: '2rem',
              maxWidth: '900px',
              margin: '0 auto 4rem auto',
            }}
          >
            {TIERS.map((tier) => (
              <div
                key={tier.name}
                style={{
                  backgroundColor: 'var(--dm-color-surface)',
                  border: tier.highlight
                    ? '2px solid var(--dm-color-primary)'
                    : '1px solid var(--dm-color-border)',
                  borderRadius: '20px',
                  padding: '2.5rem 2rem',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '1.5rem',
                  boxShadow: tier.highlight
                    ? '0 12px 32px rgba(31, 143, 116, 0.15)'
                    : 'var(--dm-shadow-card)',
                  position: 'relative',
                }}
              >
                {tier.badge && (
                  <span
                    style={{
                      position: 'absolute',
                      top: '-12px',
                      right: '24px',
                      backgroundColor: tier.highlight
                        ? 'var(--dm-color-primary)'
                        : 'var(--dm-color-surface-soft)',
                      color: tier.highlight ? '#ffffff' : 'var(--dm-color-text)',
                      border: '1px solid var(--dm-color-border)',
                      fontSize: '0.75rem',
                      fontWeight: 800,
                      padding: '0.2rem 0.65rem',
                      borderRadius: '9999px',
                      textTransform: 'uppercase',
                      letterSpacing: '0.04em',
                    }}
                  >
                    {tier.badge}
                  </span>
                )}

                <div>
                  <h2 style={{ fontSize: '1.35rem', fontWeight: 800, margin: '0 0 0.5rem 0' }}>
                    {tier.name}
                  </h2>
                  <p style={{ fontSize: '0.875rem', color: 'var(--dm-color-text-soft)', margin: 0 }}>
                    {tier.desc}
                  </p>
                </div>

                <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.35rem' }}>
                  <span style={{ fontSize: '2.5rem', fontWeight: 900, color: 'var(--dm-color-text)' }}>
                    {tier.price}
                  </span>
                  <span style={{ fontSize: '0.875rem', color: 'var(--dm-color-text-soft)' }}>
                    / {tier.frequency}
                  </span>
                </div>

                <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.75rem', flex: 1 }}>
                  {tier.features.map((feat) => (
                    <li key={feat} style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem', fontSize: '0.875rem', color: 'var(--dm-color-text)', lineHeight: 1.5 }}>
                      <span style={{ color: 'var(--dm-color-primary)', fontWeight: 700 }}>✓</span>
                      <span>{feat}</span>
                    </li>
                  ))}
                </ul>

                <Link to={tier.ctaLink} style={{ width: '100%' }}>
                  <Button variant={tier.highlight ? 'primary' : 'outline'} size="lg" style={{ width: '100%' }}>
                    {tier.cta}
                  </Button>
                </Link>
              </div>
            ))}
          </div>
        </ResponsiveContainer>
      </div>
    </Layout>
  )
}
