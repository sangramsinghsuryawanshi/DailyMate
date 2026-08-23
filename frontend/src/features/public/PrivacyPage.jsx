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

export default function PrivacyPage() {
  const { user } = useSafeAuth()
  const Layout = user ? MainLayout : PublicLayout

  return (
    <Layout>
      <div style={{ padding: '3rem 0 4rem 0' }}>
        <ResponsiveContainer size="wide">
          <div style={{ maxWidth: '800px', margin: '0 auto', backgroundColor: 'var(--dm-color-surface)', border: '1px solid var(--dm-color-border)', borderRadius: '20px', padding: '2.5rem 2rem', boxShadow: 'var(--dm-shadow-card)' }}>
            <span className="dm-section-eyebrow">Trust & Privacy</span>
            <h1 style={{ fontSize: '2rem', fontWeight: 900, margin: '0.5rem 0 1.5rem 0', color: 'var(--dm-color-text)' }}>
              DailyMate Privacy Policy
            </h1>
            <p style={{ fontSize: '0.875rem', color: 'var(--dm-color-text-soft)', marginBottom: '2rem' }}>
              Last Updated: August 2026
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', fontSize: '0.9375rem', lineHeight: 1.65, color: 'var(--dm-color-text)' }}>
              <section>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '0 0 0.5rem 0' }}>1. Information We Collect</h2>
                <p style={{ color: 'var(--dm-color-text-soft)', margin: 0 }}>
                  DailyMate collects account information (such as your name and email address) and data you explicitly provide to use our life management tools, including medication schedules, expense logs, and emergency contacts.
                </p>
              </section>

              <section>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '0 0 0.5rem 0' }}>2. How We Use Your Data</h2>
                <p style={{ color: 'var(--dm-color-text-soft)', margin: 0 }}>
                  Your data is strictly utilized to provide and personalize your DailyMate experience, schedule reminders, render analytics, and facilitate direct community and marketplace interactions. We never sell your personal data to third parties.
                </p>
              </section>

              <section>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '0 0 0.5rem 0' }}>3. Data Isolation & Security</h2>
                <p style={{ color: 'var(--dm-color-text-soft)', margin: 0 }}>
                  All personal records are encrypted in transit and isolated by authenticated user ID. We use industry-standard JWT authentication and password hashing algorithms to secure your session.
                </p>
              </section>

              <section>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 800, margin: '0 0 0.5rem 0' }}>4. Your Rights & Control</h2>
                <p style={{ color: 'var(--dm-color-text-soft)', margin: 0 }}>
                  You retain full ownership of your data. You may view, update, export, or delete your entries at any time directly through your Profile Settings.
                </p>
              </section>
            </div>
          </div>
        </ResponsiveContainer>
      </div>
    </Layout>
  )
}
