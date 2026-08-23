import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import LandingPage from '../LandingPage'

describe('LandingPage — Public Marketing & Conversion Experience', () => {
  it('renders hero with headline, value proposition, and CTA buttons', () => {
    render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { level: 1, name: /One intelligent place to manage/i })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /Get Started Free/i }).length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText(/Zero Unintended Mutations/i)).toBeInTheDocument()
    expect(screen.getByText(/100% Private Data/i)).toBeInTheDocument()
  })

  it('allows interactive switching of AI demo presets without backend API', async () => {
    const user = userEvent.setup({ delay: null })
    render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>
    )

    // Default demo preset is expense
    expect(screen.getByText('Food & Dining Analysis')).toBeInTheDocument()
    expect(screen.getByText('₹4,850')).toBeInTheDocument()

    // Switch to medicine reminder preset
    const medPresetBtn = screen.getByRole('button', { name: /Medicine Reminder/i })
    await user.click(medPresetBtn)

    expect(screen.getByText(/Remind me to take Vitamin D/i)).toBeInTheDocument()
    expect(screen.getByText(/Vitamin D \(500mg\)/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Confirm Action/i })).toBeInTheDocument()

    // Interactive confirm simulation
    await user.click(screen.getByRole('button', { name: /Confirm Action/i }))
    expect(screen.getByText(/2 reminders scheduled successfully/i)).toBeInTheDocument()
    expect(screen.getByText('EXECUTED')).toBeInTheDocument()
  })

  it('renders core product features, marketplace, pricing, and FAQ sections', async () => {
    const user = userEvent.setup({ delay: null })
    render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>
    )

    // Features
    expect(screen.getByText('Understand & Control Spending')).toBeInTheDocument()
    expect(screen.getByText('Never Miss Medication')).toBeInTheDocument()
    expect(screen.getByText('Community Support & Civic Alerts')).toBeInTheDocument()
    expect(screen.getByText('Verified Service Marketplace')).toBeInTheDocument()

    // Marketplace provider conversion
    expect(screen.getByText(/Grow your business with verified community leads/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Become a Verified Provider/i })).toBeInTheDocument()

    // Pricing
    expect(screen.getByText('Free Essential')).toBeInTheDocument()
    expect(screen.getByText('Life Assistant Pro')).toBeInTheDocument()
    expect(screen.getByText('Verified Provider')).toBeInTheDocument()

    // FAQ Accordion
    const faqBtn = screen.getByRole('button', { name: /Does the AI Assistant make changes without my permission\?/i })
    await user.click(faqBtn)
    expect(screen.getByText(/Never\. DailyMate enforces a strict zero-unintended-mutation policy/i)).toBeInTheDocument()
  })
})
