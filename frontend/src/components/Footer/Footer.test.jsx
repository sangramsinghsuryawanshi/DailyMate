import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import {
  Footer,
  FOOTER_PRODUCT_LINKS,
  FOOTER_COMMUNITY_LINKS,
  FOOTER_COMPANY_LINKS,
  FOOTER_SOCIAL_LINKS,
} from './Footer'

describe('Footer Component', () => {
  it('renders brand section with logo, title, tagline, and platform description', () => {
    render(
      <MemoryRouter>
        <Footer />
      </MemoryRouter>,
    )

    expect(screen.getByRole('contentinfo', { name: /DailyMate footer/i })).toBeInTheDocument()
    expect(screen.getByAltText('DailyMate Logo')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /DailyMate Home/i })).toBeInTheDocument()
    expect(screen.getByText('Your everyday life, organized.')).toBeInTheDocument()
    expect(
      screen.getByText(/DailyMate is an all-in-one personal life-management platform/i),
    ).toBeInTheDocument()
  })

  it('renders all product navigation links', () => {
    render(
      <MemoryRouter>
        <Footer />
      </MemoryRouter>,
    )

    const productNav = screen.getByRole('navigation', { name: /Product Links/i })
    expect(productNav).toBeInTheDocument()

    FOOTER_PRODUCT_LINKS.forEach((item) => {
      expect(screen.getByRole('link', { name: item.label })).toHaveAttribute('href', item.href)
    })
  })

  it('renders all community navigation links', () => {
    render(
      <MemoryRouter>
        <Footer />
      </MemoryRouter>,
    )

    const communityNav = screen.getByRole('navigation', { name: /Community Links/i })
    expect(communityNav).toBeInTheDocument()

    FOOTER_COMMUNITY_LINKS.forEach((item) => {
      expect(screen.getByRole('link', { name: item.label })).toHaveAttribute('href', item.href)
    })
  })

  it('renders all company and support links', () => {
    render(
      <MemoryRouter>
        <Footer />
      </MemoryRouter>,
    )

    const companyNav = screen.getByRole('navigation', { name: /Corporate Directory/i })
    expect(companyNav).toBeInTheDocument()

    FOOTER_COMPANY_LINKS.forEach((item) => {
      expect(screen.getAllByRole('link', { name: item.label }).length).toBeGreaterThan(0)
    })
  })

  it('renders social icons with accessible labels', () => {
    render(
      <MemoryRouter>
        <Footer />
      </MemoryRouter>,
    )

    FOOTER_SOCIAL_LINKS.forEach((s) => {
      const socialBtn = screen.getByRole('link', { name: s.ariaLabel })
      expect(socialBtn).toBeInTheDocument()
      expect(socialBtn).toHaveAttribute('href', s.href)
    })
  })

  it('renders bottom bar with copyright, legal links, and handles cookie preferences click', async () => {
    const onCookiePreferencesMock = vi.fn()
    const user = userEvent.setup({ delay: null })

    render(
      <MemoryRouter>
        <Footer onCookiePreferencesClick={onCookiePreferencesMock} />
      </MemoryRouter>,
    )

    expect(screen.getByText(/© 2026 DailyMate\. All rights reserved\./i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cookie Preferences' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Cookie Preferences' }))
    expect(onCookiePreferencesMock).toHaveBeenCalledTimes(1)
  })
})
