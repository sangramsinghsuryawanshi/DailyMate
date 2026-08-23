import { useState } from 'react'
import { Button, Input, ResponsiveContainer } from '../../design-system'
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

export default function ContactPage() {
  const { user } = useSafeAuth()
  const Layout = user ? MainLayout : PublicLayout

  const [submitted, setSubmitted] = useState(false)
  const [formData, setFormData] = useState({ name: '', email: '', subject: '', message: '' })

  const handleSubmit = (e) => {
    e.preventDefault()
    setSubmitted(true)
  }

  return (
    <Layout>
      <div style={{ padding: '3rem 0 4rem 0' }}>
        <ResponsiveContainer size="wide">
          <div style={{ textAlign: 'center', maxWidth: '760px', margin: '0 auto 3.5rem auto' }}>
            <span className="dm-section-eyebrow">Get In Touch</span>
            <h1 className="dm-page-main-title" style={{ fontSize: 'clamp(2rem, 4vw, 3rem)' }}>
              We’d love to hear from you
            </h1>
            <p className="dm-page-subtitle" style={{ fontSize: '1.125rem' }}>
              Have questions, feedback, or need help with your DailyMate account? Reach out to our team.
            </p>
          </div>

          <div
            style={{
              maxWidth: '680px',
              margin: '0 auto',
              backgroundColor: 'var(--dm-color-surface)',
              border: '1px solid var(--dm-color-border)',
              borderRadius: '20px',
              padding: '2.5rem 2rem',
              boxShadow: 'var(--dm-shadow-card)',
            }}
          >
            {submitted ? (
              <div style={{ textAlign: 'center', padding: '2rem 1rem' }}>
                <span style={{ fontSize: '3rem' }}>✉️</span>
                <h3 style={{ fontSize: '1.5rem', fontWeight: 800, marginTop: '1rem', color: 'var(--dm-color-text)' }}>
                  Message Sent Successfully
                </h3>
                <p style={{ color: 'var(--dm-color-text-soft)', marginTop: '0.5rem' }}>
                  Thank you for reaching out. A member of our support team will respond to {formData.email || 'your email'} shortly.
                </p>
                <Button variant="outline" size="md" onClick={() => setSubmitted(false)} style={{ marginTop: '1.5rem' }}>
                  Send Another Message
                </Button>
              </div>
            ) : (
              <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                <Input
                  label="Your Name"
                  placeholder="e.g. Sangram Singh"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                />
                <Input
                  label="Email Address"
                  type="email"
                  placeholder="e.g. name@example.com"
                  required
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                />
                <Input
                  label="Subject"
                  placeholder="e.g. Marketplace inquiry, Feature request"
                  required
                  value={formData.subject}
                  onChange={(e) => setFormData({ ...formData, subject: e.target.value })}
                />
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
                  <label style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--dm-color-text)' }}>
                    Message
                  </label>
                  <textarea
                    rows={5}
                    required
                    value={formData.message}
                    onChange={(e) => setFormData({ ...formData, message: e.target.value })}
                    placeholder="How can we help you today?"
                    style={{
                      padding: '0.75rem',
                      borderRadius: '8px',
                      border: '1px solid var(--dm-color-border)',
                      backgroundColor: 'var(--dm-color-surface-soft)',
                      color: 'var(--dm-color-text)',
                      fontFamily: 'inherit',
                      fontSize: '0.875rem',
                    }}
                  />
                </div>

                <Button type="submit" variant="primary" size="lg" style={{ marginTop: '0.5rem', width: '100%' }}>
                  Submit Inquiry
                </Button>
              </form>
            )}
          </div>
        </ResponsiveContainer>
      </div>
    </Layout>
  )
}
