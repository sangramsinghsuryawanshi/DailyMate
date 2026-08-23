import { ResponsiveContainer } from '../../design-system'
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

export default function TermsPage() {
  const { user } = useSafeAuth()
  const Layout = user ? MainLayout : PublicLayout

  return (
    <Layout>
      <div style={{ padding: '3rem 0 4rem 0' }}>
        <ResponsiveContainer size="wide">
          <div style={{ maxWidth: '800px', margin: '0 auto', backgroundColor: 'var(--dm-color-surface)', border: '1px solid var(--dm-color-border)', borderRadius: '20px', padding: '2.5rem 2rem', boxShadow: 'var(--dm-shadow-card)' }}>
            <span className="dm-section-eyebrow">Legal Agreement</span>
            <h1 style={{ fontSize: '2rem', fontWeight: 900, margin: '0.5rem 0 1.5rem 0', color: 'var(--dm-color-text)' }}>
              DailyMate Terms of Service
            </h1>
            <p style={{ fontSize: '0.875rem', color: 'var(--dm-color-text-soft)', marginBottom: '2rem' }}>
              Last Updated: August 2026
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', fontSize: '0.9375rem', lineHeight: 1.65, color: 'var(--dm-color-text)' }}>
              <section>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '0 0 0.5rem 0' }}>1. Acceptance of Terms</h2>
                <p style={{ color: 'var(--dm-color-text-soft)', margin: 0 }}>
                  By accessing or using DailyMate, you agree to be bound by these Terms of Service and our Privacy Policy. If you do not agree, please do not use the platform.
                </p>
              </section>

              <section>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '0 0 0.5rem 0' }}>2. Platform Use & Account Responsibility</h2>
                <p style={{ color: 'var(--dm-color-text-soft)', margin: 0 }}>
                  You are responsible for maintaining the confidentiality of your account credentials and for all activities that occur under your account.
                </p>
              </section>

              <section>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '0 0 0.5rem 0' }}>3. Marketplace & Community Conduct</h2>
                <p style={{ color: 'var(--dm-color-text-soft)', margin: 0 }}>
                  DailyMate provides direct connections between users and service providers. Users must provide truthful information when posting blood donation appeals, civic complaints, lost items, or marketplace reviews.
                </p>
              </section>

              <section>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '0 0 0.5rem 0' }}>4. AI Assistant Invariants</h2>
                <p style={{ color: 'var(--dm-color-text-soft)', margin: 0 }}>
                  The DailyMate AI Assistant provides automated life organization assistance and explicit Action Proposals. It does not provide certified medical diagnoses or legal advice.
                </p>
              </section>
            </div>
          </div>
        </ResponsiveContainer>
      </div>
    </Layout>
  )
}
