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

const FAQS = [
  {
    q: 'What is DailyMate?',
    a: 'DailyMate is an all-in-one personal life-management platform that consolidates medication reminders, expense logging, emergency ICE contacts, local marketplace providers, community services, and AI assistance into one connected workspace.',
  },
  {
    q: 'Is my personal healthcare and financial information private?',
    a: 'Yes. DailyMate uses strict user isolation, authenticated JWT sessions, and zero third-party data sales. Your entries are visible only to your authenticated account.',
  },
  {
    q: 'Are local service providers on DailyMate charged commissions?',
    a: 'No. DailyMate is a zero-fabrication, direct-contact marketplace. Users contact providers directly via verified phone and email without middleman booking commissions.',
  },
  {
    q: 'How does the AI Assistant execute actions?',
    a: 'The DailyMate Assistant uses safe Action Proposals. When you ask the assistant to log an expense or set a reminder, it proposes a structured action that requires your explicit one-click approval before any database record is created.',
  },
  {
    q: 'Can I access DailyMate on both mobile and desktop?',
    a: 'Yes. DailyMate is fully responsive across mobile phones, tablets, and desktop browsers with adaptive layouts, touch gestures, and keyboard navigation.',
  },
]

export default function FaqPage() {
  const { user } = useSafeAuth()
  const Layout = user ? MainLayout : PublicLayout

  return (
    <Layout>
      <div style={{ padding: '3rem 0 4rem 0' }}>
        <ResponsiveContainer size="wide">
          <div style={{ textAlign: 'center', maxWidth: '760px', margin: '0 auto 3.5rem auto' }}>
            <span className="dm-section-eyebrow">Frequently Asked Questions</span>
            <h1 className="dm-page-main-title" style={{ fontSize: 'clamp(2rem, 4vw, 3rem)' }}>
              Everything you need to know about DailyMate
            </h1>
            <p className="dm-page-subtitle" style={{ fontSize: '1.125rem' }}>
              Common questions about our features, security, marketplace, and platform capabilities.
            </p>
          </div>

          <div
            style={{
              maxWidth: '800px',
              margin: '0 auto 4rem auto',
              display: 'flex',
              flexDirection: 'column',
              gap: '1.25rem',
            }}
          >
            {FAQS.map((faq) => (
              <div
                key={faq.q}
                style={{
                  backgroundColor: 'var(--dm-color-surface)',
                  border: '1px solid var(--dm-color-border)',
                  borderRadius: '16px',
                  padding: '1.75rem',
                  boxShadow: 'var(--dm-shadow-card)',
                }}
              >
                <h3 style={{ fontSize: '1.125rem', fontWeight: 800, margin: '0 0 0.75rem 0', color: 'var(--dm-color-text)' }}>
                  {faq.q}
                </h3>
                <p style={{ fontSize: '0.9375rem', lineHeight: 1.6, color: 'var(--dm-color-text-soft)', margin: 0 }}>
                  {faq.a}
                </p>
              </div>
            ))}
          </div>

          <div style={{ textAlign: 'center' }}>
            <p style={{ color: 'var(--dm-color-text-soft)', marginBottom: '1rem' }}>
              Have more questions? We are always here to help.
            </p>
            <Link to="/contact">
              <Button variant="outline" size="md">
                Contact Support
              </Button>
            </Link>
          </div>
        </ResponsiveContainer>
      </div>
    </Layout>
  )
}
