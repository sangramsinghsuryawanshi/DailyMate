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

const STEPS = [
  {
    step: '01',
    title: 'Create & Secure Your Account',
    desc: 'Register in seconds with email verification and profile personalization. All personal entries are protected under your private authenticated session.',
    icon: '🔐',
  },
  {
    step: '02',
    title: 'Set Up Routines & Family ICE',
    desc: 'Add your medication reminders, configure monthly spending categories, and register your emergency ICE contacts for rapid one-tap crisis dialing.',
    icon: '⚙️',
  },
  {
    step: '03',
    title: 'Connect with Your Community',
    desc: 'Search local services with zero middleman fees, respond to urgent neighborhood blood requests, file civic complaints, and post lost items.',
    icon: '🤝',
  },
  {
    step: '04',
    title: 'Automate & Assist with AI',
    desc: 'Use natural-language prompts to log transactions and create reminders with safe Action Proposals that require your explicit one-click confirmation.',
    icon: '✨',
  },
]

export default function HowItWorksPage() {
  const { user } = useSafeAuth()
  const Layout = user ? MainLayout : PublicLayout

  return (
    <Layout>
      <div style={{ padding: '3rem 0 4rem 0' }}>
        <ResponsiveContainer size="wide">
          <div style={{ textAlign: 'center', maxWidth: '760px', margin: '0 auto 3.5rem auto' }}>
            <span className="dm-section-eyebrow">Platform Flow</span>
            <h1 className="dm-page-main-title" style={{ fontSize: 'clamp(2rem, 4vw, 3rem)' }}>
              How DailyMate Works
            </h1>
            <p className="dm-page-subtitle" style={{ fontSize: '1.125rem' }}>
              Four streamlined steps to simplify, organize, and connect your everyday life.
            </p>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
              gap: '1.5rem',
              maxWidth: '1100px',
              margin: '0 auto 4rem auto',
            }}
          >
            {STEPS.map((s) => (
              <div
                key={s.step}
                style={{
                  backgroundColor: 'var(--dm-color-surface)',
                  border: '1px solid var(--dm-color-border)',
                  borderRadius: '20px',
                  padding: '2rem 1.75rem',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '1rem',
                  boxShadow: 'var(--dm-shadow-card)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '2rem' }}>{s.icon}</span>
                  <span style={{ fontSize: '1.5rem', fontWeight: 900, color: 'var(--dm-color-primary)' }}>
                    {s.step}
                  </span>
                </div>
                <h3 style={{ fontSize: '1.1875rem', fontWeight: 800, margin: 0, color: 'var(--dm-color-text)' }}>
                  {s.title}
                </h3>
                <p style={{ fontSize: '0.875rem', lineHeight: 1.6, color: 'var(--dm-color-text-soft)', margin: 0 }}>
                  {s.desc}
                </p>
              </div>
            ))}
          </div>

          <div style={{ textAlign: 'center' }}>
            <Link to="/register">
              <Button variant="primary" size="lg">
                Get Started with DailyMate
              </Button>
            </Link>
          </div>
        </ResponsiveContainer>
      </div>
    </Layout>
  )
}
