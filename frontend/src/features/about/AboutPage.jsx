import { Link } from 'react-router-dom'
import {
  Button,
  ResponsiveContainer,
} from '../../design-system'
import PublicLayout from '../../layouts/PublicLayout'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'
import './AboutPage.css'

function useSafeAuth() {
  try {
    return useAuth()
  } catch {
    return { user: null }
  }
}

const HERO_PILLS = [
  { icon: '📅', title: 'Healthcare Routines', desc: 'Medicine schedules & adherence' },
  { icon: '💰', title: 'Financial Clarity', desc: 'Spending tracking & budgets' },
  { icon: '🛠️', title: 'Local Marketplace', desc: 'Direct-contact verified pros' },
  { icon: '✨', title: 'Intelligent AI', desc: 'Controlled action proposals' },
]

const PROBLEMS_BEFORE = [
  'Too many disconnected apps for everyday needs',
  'Important medication reminders getting missed',
  'Managing expenses across fragmented spreadsheets',
  'Difficulty finding verified, commission-free local services',
  'Critical emergency contacts buried in generic phonebooks',
  'Community notices, blood requests, and civic alerts scattered across chat groups',
]

const SOLUTIONS_WITH = [
  'One unified platform connecting daily life essentials',
  'Timely reminders with zero medical diagnosis ambiguity',
  'Integrated monthly transaction tracking & analytics',
  'Direct-contact service directory with 100% zero commissions',
  'Instant ICE hotlines with one-tap calling & SMS alerts',
  'Real-time neighborhood community hub with complaints, lost & found, and events',
]

const CORE_CAPABILITIES = [
  {
    icon: '💊',
    title: 'Personal Healthcare Routines',
    desc: 'Schedule dosage alerts, track medication adherence, and maintain structured health schedules effortlessly.',
  },
  {
    icon: '💰',
    title: 'Financial Management',
    desc: 'Log expenses, inspect monthly category totals, analyze spending patterns, and maintain realistic savings budgets.',
  },
  {
    icon: '🛠️',
    title: 'Local Services Marketplace',
    desc: 'Discover verified neighborhood plumbers, electricians, tutors, and technicians with direct phone and email contact.',
  },
  {
    icon: '🩸',
    title: 'Community Care & Blood Appeals',
    desc: 'Post urgent blood appeals, organize local events, file community complaints, and reunite lost and found items.',
  },
  {
    icon: '🚨',
    title: 'Emergency ICE Directory',
    desc: 'One-tap access to family ICE contacts, emergency services (112, 102, 101), and immediate SMS dispatch.',
  },
  {
    icon: '✨',
    title: 'Controlled AI Assistance',
    desc: 'Natural language assistance with safe Action Proposals requiring explicit user approval before database mutations.',
  },
  {
    icon: '🔔',
    title: 'Centralized Notifications',
    desc: 'Unified notifications and activity alerts ensuring critical updates are never missed or buried.',
  },
]

const CORE_VALUES = [
  {
    title: 'Simplicity',
    desc: 'Make everyday life simpler and less fragmented rather than adding unnecessary complexity or clutter.',
  },
  {
    title: 'Trust & Privacy',
    desc: 'Protect user data with strict isolation, secure authentication, and zero fabricated claims.',
  },
  {
    title: 'Community',
    desc: 'Connect neighbors with verified local providers, blood donors, civic solutions, and local events.',
  },
  {
    title: 'Responsible Intelligence',
    desc: 'Use AI purposefully with transparent action proposals that require explicit user confirmation.',
  },
  {
    title: 'Accessibility',
    desc: 'Design inclusive, responsive interfaces with clear contrast, screen-reader support, and touch accessibility.',
  },
  {
    title: 'Continuous Improvement',
    desc: 'Build, iterate, and refine capabilities based on authentic user feedback and everyday real-world utility.',
  },
]

const HOW_IT_WORKS = [
  {
    step: '01',
    title: 'Create your account',
    desc: 'Set up your secure DailyMate account in seconds with your verified email and profile details.',
  },
  {
    step: '02',
    title: 'Personalize your workspace',
    desc: 'Configure medication schedules, set financial budget goals, and register emergency ICE contacts.',
  },
  {
    step: '03',
    title: 'Organize your everyday life',
    desc: 'Use connected tools for daily expenses, grocery comparison, local jobs, and neighborhood civic alerts.',
  },
  {
    step: '04',
    title: 'Get smarter assistance',
    desc: 'Leverage global search (⌘K), real-time notifications, and conversational AI assistance on demand.',
  },
]

const TECH_STACK = [
  'Java 21',
  'Spring Boot 3',
  'Spring Security',
  'JWT Auth',
  'Spring Data JPA',
  'MySQL',
  'Flyway Migrations',
  'React 19',
  'Vite',
  'TanStack Query',
  'Axios',
  'Tailwind CSS',
]

const ABOUT_SUBNAV = [
  { label: 'Overview', href: '#about-overview' },
  { label: 'The Problem', href: '#the-problem' },
  { label: 'Vision & Mission', href: '#vision-mission' },
  { label: 'Capabilities', href: '#capabilities' },
  { label: 'Values', href: '#values' },
  { label: 'How It Works', href: '#how-it-works' },
  { label: 'Trust & Tech', href: '#trust-tech' },
]

export default function AboutPage() {
  const { user } = useSafeAuth()
  const Layout = user ? MainLayout : PublicLayout

  const handleAnchorClick = (e, targetId) => {
    e.preventDefault()
    const target = document.getElementById(targetId)
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }

  return (
    <Layout>
      <div className="dm-about-page">
        <ResponsiveContainer size="wide">
          {/* 1. Hero Section */}
          <section className="dm-about-hero" aria-labelledby="about-hero-title">
            <span className="dm-about-hero__badge">About DailyMate</span>
            <h1 id="about-hero-title" className="dm-about-hero__title">
              One place for everything that matters in your everyday life.
            </h1>
            <p className="dm-about-hero__subtitle">
              DailyMate is an all-in-one life-management platform bringing personal routines, healthcare schedules,
              financial tracking, local services, community needs, and AI assistance together into one connected experience.
            </p>
            <div className="dm-about-hero__actions">
              <Link to={user ? "/dashboard" : "/register"}>
                <Button variant="primary" size="lg">
                  {user ? "Open Dashboard" : "Get Started"}
                </Button>
              </Link>
              <a
                href="#capabilities"
                onClick={(e) => handleAnchorClick(e, "capabilities")}
              >
                <Button variant="outline" size="lg">
                  Explore Capabilities
                </Button>
              </a>
            </div>
          </section>

          {/* In-Page Quick Navigation Bar */}
          <nav
            aria-label="About Page Sections"
            style={{
              display: 'flex',
              justifyContent: 'center',
              flexWrap: 'wrap',
              gap: '0.5rem',
              marginBottom: '3rem',
              padding: '0.5rem',
              borderRadius: '9999px',
              backgroundColor: 'var(--dm-color-surface)',
              border: '1px solid var(--dm-color-border)',
              maxWidth: '820px',
              margin: '0 auto 3.5rem auto',
            }}
          >
            {ABOUT_SUBNAV.map((sub) => (
              <a
                key={sub.label}
                href={sub.href}
                onClick={(e) => handleAnchorClick(e, sub.href.substring(1))}
                style={{
                  fontSize: '0.8125rem',
                  fontWeight: 600,
                  color: 'var(--dm-color-text-soft)',
                  textDecoration: 'none',
                  padding: '0.35rem 0.75rem',
                  borderRadius: '9999px',
                  transition: 'all 0.15s ease',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.color = 'var(--dm-color-primary)'
                  e.currentTarget.style.backgroundColor = 'var(--dm-color-surface-soft)'
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.color = 'var(--dm-color-text-soft)'
                  e.currentTarget.style.backgroundColor = 'transparent'
                }}
              >
                {sub.label}
              </a>
            ))}
          </nav>

          {/* Hero Visual Cards */}
          <div className="dm-about-hero-graphic" aria-label="DailyMate Core Pillars">
            {HERO_PILLS.map((pill) => (
              <div key={pill.title} className="dm-about-hero-pill">
                <span className="dm-about-hero-pill__icon" aria-hidden="true">
                  {pill.icon}
                </span>
                <div className="dm-about-hero-pill__text">
                  <strong>{pill.title}</strong>
                  <span>{pill.desc}</span>
                </div>
              </div>
            ))}
          </div>

          {/* 2. What Is DailyMate */}
          <section id="about-overview" className="dm-about-overview-card" aria-labelledby="what-is-title">
            <h2 id="what-is-title" style={{ fontSize: '1.75rem', fontWeight: 800, margin: '0 0 1rem 0' }}>
              What Is DailyMate?
            </h2>
            <p className="dm-about-overview-lead">
              DailyMate brings the tools you need for everyday life into one place.
            </p>
            <p className="dm-about-overview-copy">
              Instead of switching between dozens of single-purpose apps for reminders, expenses, grocery prices,
              local service providers, community alerts, emergency contacts, and AI assistance, DailyMate creates
              one connected experience designed around your daily life.
            </p>
          </section>

          {/* 3. The Problem We Solve */}
          <section id="the-problem" style={{ marginBottom: '4rem', scrollMarginTop: '80px' }} aria-labelledby="problem-solution-title">
            <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
              <span className="dm-section-eyebrow">The Problem We Solve</span>
              <h2 id="problem-solution-title" className="dm-page-main-title">
                From fragmented apps to connected living
              </h2>
              <p className="dm-page-subtitle">
                How DailyMate transforms scattered daily friction into an organized life.
              </p>
            </div>

            <div className="dm-about-comparison-grid">
              {/* Before Card */}
              <div className="dm-about-comparison-card dm-about-comparison-card--before">
                <span className="dm-about-comparison-badge dm-about-comparison-badge--red">
                  ⚠️ Before DailyMate
                </span>
                <h3 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 800 }}>
                  Fragmented & Overwhelming
                </h3>
                <ul className="dm-about-list">
                  {PROBLEMS_BEFORE.map((item) => (
                    <li key={item} className="dm-about-list-item">
                      <span className="dm-about-list-item__icon" aria-hidden="true">❌</span>
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
              </div>

              {/* With Card */}
              <div className="dm-about-comparison-card dm-about-comparison-card--after">
                <span className="dm-about-comparison-badge dm-about-comparison-badge--green">
                  ✓ With DailyMate
                </span>
                <h3 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 800 }}>
                  Connected & Organized
                </h3>
                <ul className="dm-about-list">
                  {SOLUTIONS_WITH.map((item) => (
                    <li key={item} className="dm-about-list-item">
                      <span className="dm-about-list-item__icon" aria-hidden="true">✅</span>
                      <span>{item}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </section>

          {/* 4 & 5. Vision and Mission */}
          <section id="vision-mission" className="dm-about-vm-grid" style={{ scrollMarginTop: '80px' }} aria-label="Vision and Mission">
            <div className="dm-about-vm-card">
              <span className="dm-section-eyebrow">Our Vision</span>
              <h2>A simpler, more intelligent everyday life</h2>
              <p>
                DailyMate’s vision is to create a world where managing everyday life is simpler, more connected,
                and more intelligent. We believe people should spend less time juggling disconnected tools and more time
                focusing on what truly matters to their families and communities.
              </p>
            </div>

            <div className="dm-about-vm-card">
              <span className="dm-section-eyebrow">Our Mission</span>
              <h2>How we build toward our vision</h2>
              <p>
                Our mission is to unify essential daily services, make trusted information instant to access,
                empower local community support, and integrate assistive AI responsibly within a secure, privacy-first platform.
              </p>
            </div>
          </section>

          {/* 6. What You Can Do With DailyMate */}
          <section id="capabilities" style={{ marginBottom: '4rem', scrollMarginTop: '80px' }} aria-labelledby="capabilities-title">
            <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
              <span className="dm-section-eyebrow">Key Capabilities</span>
              <h2 id="capabilities-title" className="dm-page-main-title">
                Everything you need to run your day
              </h2>
              <p className="dm-page-subtitle">
                Explore the connected modules built into the DailyMate platform.
              </p>
            </div>

            <div className="dm-about-caps-grid">
              {CORE_CAPABILITIES.map((cap) => (
                <div key={cap.title} className="dm-about-cap-card">
                  <span className="dm-about-cap-card__icon" aria-hidden="true">
                    {cap.icon}
                  </span>
                  <h3 className="dm-about-cap-card__title">{cap.title}</h3>
                  <p className="dm-about-cap-card__desc">{cap.desc}</p>
                </div>
              ))}
            </div>
          </section>

          {/* 7. Core Values */}
          <section id="values" style={{ marginBottom: '4rem', scrollMarginTop: '80px' }} aria-labelledby="values-title">
            <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
              <span className="dm-section-eyebrow">Core Principles</span>
              <h2 id="values-title" className="dm-page-main-title">
                The values that guide DailyMate
              </h2>
              <p className="dm-page-subtitle">
                Foundational standards built into every feature and architecture decision.
              </p>
            </div>

            <div className="dm-about-values-grid">
              {CORE_VALUES.map((val) => (
                <div key={val.title} className="dm-about-value-card">
                  <h3 className="dm-about-value-card__title">{val.title}</h3>
                  <p className="dm-about-value-card__desc">{val.desc}</p>
                </div>
              ))}
            </div>
          </section>

          {/* 8. How DailyMate Works */}
          <section id="how-it-works" style={{ marginBottom: '4rem', scrollMarginTop: '80px' }} aria-labelledby="how-it-works-title">
            <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
              <span className="dm-section-eyebrow">Workflow</span>
              <h2 id="how-it-works-title" className="dm-page-main-title">
                How DailyMate Works
              </h2>
              <p className="dm-page-subtitle">
                A simple, 4-step path to an organized life.
              </p>
            </div>

            <div className="dm-about-steps-grid">
              {HOW_IT_WORKS.map((step) => (
                <div key={step.step} className="dm-about-step-card">
                  <span className="dm-about-step-number">{step.step}</span>
                  <h3 className="dm-about-step-title">{step.title}</h3>
                  <p className="dm-about-step-desc">{step.desc}</p>
                </div>
              ))}
            </div>
          </section>

          {/* 9 & 10. Trust, Privacy & Tech Stack */}
          <section id="trust-tech" className="dm-about-trust-grid" style={{ scrollMarginTop: '80px' }} aria-label="Trust, Privacy and Technology">
            <div className="dm-about-trust-card">
              <span className="dm-section-eyebrow">Trust & Privacy</span>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 800, margin: 0 }}>
                Built with privacy-conscious security
              </h2>
              <p style={{ fontSize: '0.9375rem', lineHeight: 1.6, color: 'var(--dm-color-text-soft)', margin: 0 }}>
                Your personal health entries, financial transactions, and ICE contacts are strictly isolated to your authenticated container.
                DailyMate enforces token revocation, encrypted data handling, and zero unauthorized mutations.
              </p>
              <div style={{ display: 'flex', gap: '1rem', marginTop: '0.5rem', flexWrap: 'wrap' }}>
                <Link to="/privacy" className="dm-footer__legal-link" style={{ fontWeight: 600 }}>
                  Privacy Policy →
                </Link>
                <Link to="/terms" className="dm-footer__legal-link" style={{ fontWeight: 600 }}>
                  Terms of Service →
                </Link>
              </div>
            </div>

            <div className="dm-about-trust-card">
              <span className="dm-section-eyebrow">Architecture & Tech</span>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 800, margin: 0 }}>
                Enterprise-grade modern stack
              </h2>
              <p style={{ fontSize: '0.9375rem', lineHeight: 1.6, color: 'var(--dm-color-text-soft)', margin: 0 }}>
                Built on a reliable, performant technology stack engineered for high responsiveness and stability.
              </p>
              <div className="dm-about-tech-tags">
                {TECH_STACK.map((tech) => (
                  <span key={tech} className="dm-about-tech-pill">
                    {tech}
                  </span>
                ))}
              </div>
            </div>
          </section>

          {/* 11. Call to Action Banner */}
          <section className="dm-about-cta-card" aria-labelledby="cta-banner-title">
            <h2 id="cta-banner-title">Make everyday life a little easier.</h2>
            <p>
              Discover a simpler way to organize, manage, and connect the things that matter every day.
            </p>
            <Link to={user ? "/dashboard" : "/register"} style={{ marginTop: '0.5rem' }}>
              <Button variant="secondary" size="lg">
                {user ? "Return to Dashboard" : "Get Started with DailyMate"}
              </Button>
            </Link>
          </section>
        </ResponsiveContainer>
      </div>
    </Layout>
  )
}
