import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button, Badge, Card, StatCard, TrustBadge, ResponsiveContainer } from '../../design-system'
import PublicLayout from '../../layouts/PublicLayout'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import './LandingPage.css'

function useSafeAuth() {
  try {
    return useAuth()
  } catch {
    return { user: null }
  }
}

const DEMO_PRESETS = [
  {
    id: 'expense',
    label: '💰 Expense Query',
    prompt: 'How much did I spend on food this month?',
    responseType: 'expense',
    response: {
      title: 'Food & Dining Analysis',
      total: '₹4,850',
      change: '18% less than last month',
      items: [
        { name: 'Khichadi & Afternoon Lunch', cost: '₹50' },
        { name: 'Weekly Groceries (Supermart)', cost: '₹1,500' },
        { name: 'Dinner & Family Meal', cost: '₹800' },
      ],
      insight: 'You are on track to save ₹2,150 from your monthly budget goal.',
    },
  },
  {
    id: 'medicine',
    label: '💊 Medicine Reminder',
    prompt: 'Remind me to take Vitamin D at 9 PM and Calcium at 8 AM',
    responseType: 'action_proposal',
    proposal: {
      actionId: 'demo-med-1',
      title: 'Create 2 Medicine Reminders',
      summary: 'Vitamin D (500mg) at 9:00 PM · Calcium (250mg) at 8:00 AM',
      status: 'PENDING',
    },
  },
  {
    id: 'provider',
    label: '🛠️ Find Plumber',
    prompt: 'Find a reliable plumber with 4.5+ rating near Kothrud',
    responseType: 'provider',
    provider: {
      name: 'Ramesh Quickfix Services',
      service: 'Master Plumber & Pipe Fitter',
      rating: '4.9 ★ (128 reviews)',
      location: 'Kothrud, Pune (1.2 km away)',
      price: '₹300 visit charge',
      verified: true,
    },
  },
  {
    id: 'blood',
    label: '🩸 Blood Appeal',
    prompt: 'Show urgent O positive blood requests near me',
    responseType: 'blood',
    request: {
      patient: 'Kavita Patel',
      bloodGroup: 'O+',
      units: '2 Units Needed',
      hospital: 'Sahyadri Specialty Hospital',
      urgency: 'URGENT',
    },
  },
]

const FAQS = [
  {
    q: 'How does DailyMate differ from traditional productivity apps?',
    a: 'Traditional apps isolate your spending in one app, medicine reminders in another, and community alerts in social media. DailyMate integrates your personal finances, healthcare routines, community services, and an intelligent AI assistant into one fast, connected ecosystem.',
  },
  {
    q: 'Does the AI Assistant make changes without my permission?',
    a: 'Never. DailyMate enforces a strict zero-unintended-mutation policy. The AI assistant prepares structured Action Proposals in a PENDING state. Records are only written to the database when you explicitly click Confirm Action.',
  },
  {
    q: 'Is my personal health and financial data secure?',
    a: 'Yes. DailyMate uses isolated user state containers, JWT authentication with token revocation, and strict authorization rules to ensure your private entries are accessible only by you.',
  },
  {
    q: 'Can local business providers join the DailyMate Marketplace?',
    a: 'Yes. Verified local service providers (plumbers, tutors, electricians, healthcare aids) can register their profile to receive verified leads directly from local community members.',
  },
]

export default function LandingPage() {
  const navigate = useNavigate()
  const { user } = useSafeAuth()
  const Layout = user ? MainLayout : PublicLayout

  const [activeDemo, setActiveDemo] = useState(DEMO_PRESETS[0])
  const [demoConfirmed, setDemoConfirmed] = useState(false)
  const [openFaqIndex, setOpenFaqIndex] = useState(null)

  const handleCtaClick = (source) => {
    trackEvent(AnalyticsEvents.SIGNUP_STARTED, { source })
    navigate(user ? '/dashboard' : '/register')
  }

  const handlePresetSelect = (preset) => {
    setActiveDemo(preset)
    setDemoConfirmed(false)
    trackEvent('demo_prompt_clicked', { prompt: preset.label })
  }

  const toggleFaq = (index) => {
    setOpenFaqIndex((prev) => (prev === index ? null : index))
  }

  return (
    <Layout>
      <div className="dm-landing">
        {/* ===================================================================
            HERO SECTION
           =================================================================== */}
        <section className="dm-landing-hero">
          <ResponsiveContainer size="wide">
            <div className="dm-landing-hero__grid">
              <div className="dm-landing-hero__copy">
                <div className="dm-landing-hero__badge">
                  <Badge variant="default" dot>Next-Generation Life Platform</Badge>
                </div>
                <h1 className="dm-landing-hero__title">
                  One intelligent place to manage <span className="dm-gradient-text">everyday life</span>.
                </h1>
                <p className="dm-landing-hero__subtitle">
                  DailyMate unites personal expenses, medicine routines, emergency contacts, civic community alerts, verified local services, and an autonomous AI assistant in one fast, private command center.
                </p>

                <div className="dm-landing-hero__actions">
                  <Button
                    variant="primary"
                    size="lg"
                    onClick={() => handleCtaClick('hero_primary')}
                    iconRight="→"
                  >
                    Get Started Free
                  </Button>
                  <a href="#ai-demo" className="dm-landing-hero__secondary-link">
                    <Button variant="outline" size="lg">
                      See Interactive Demo
                    </Button>
                  </a>
                </div>

                <div className="dm-landing-hero__trust-strip">
                  <TrustBadge type="saved" label="Zero Unintended Mutations" />
                  <TrustBadge type="private" label="100% Private Data" />
                  <TrustBadge type="verified" label="Verified Community Hub" />
                </div>
              </div>

              {/* Interactive Live AI Demo Preview */}
              <div id="ai-demo" className="dm-landing-hero__demo-card">
                <div className="dm-demo-card">
                  <div className="dm-demo-card__header">
                    <div className="dm-demo-card__header-left">
                      <span className="dm-demo-card__bot-avatar">✨</span>
                      <div>
                        <strong>DailyMate AI Assistant</strong>
                        <span className="dm-demo-card__status">Interactive Preview</span>
                      </div>
                    </div>
                    <Badge variant="success" dot size="sm">Online</Badge>
                  </div>

                  {/* Preset Prompt Selector Pills */}
                  <div className="dm-demo-card__pills">
                    {DEMO_PRESETS.map((preset) => (
                      <button
                        key={preset.id}
                        type="button"
                        className={`dm-demo-pill ${activeDemo.id === preset.id ? 'active' : ''}`}
                        onClick={() => handlePresetSelect(preset)}
                      >
                        {preset.label}
                      </button>
                    ))}
                  </div>

                  {/* Conversation Window */}
                  <div className="dm-demo-card__chat">
                    <div className="dm-demo-bubble dm-demo-bubble--user">
                      <span>{activeDemo.prompt}</span>
                    </div>

                    <div className="dm-demo-bubble dm-demo-bubble--assistant">
                      {activeDemo.responseType === 'expense' && (
                        <div className="dm-demo-result dm-demo-result--expense">
                          <div className="dm-demo-result__header">
                            <strong>{activeDemo.response.title}</strong>
                            <span className="dm-demo-result__cost">{activeDemo.response.total}</span>
                          </div>
                          <span className="dm-demo-result__change">{activeDemo.response.change}</span>
                          <ul className="dm-demo-result__list">
                            {activeDemo.response.items.map((item, idx) => (
                              <li key={idx}>
                                <span>{item.name}</span>
                                <strong>{item.cost}</strong>
                              </li>
                            ))}
                          </ul>
                          <div className="dm-demo-result__insight">
                            💡 {activeDemo.response.insight}
                          </div>
                        </div>
                      )}

                      {activeDemo.responseType === 'action_proposal' && (
                        <div className="dm-demo-result dm-demo-result--proposal">
                          <div className="dm-demo-result__header">
                            <strong>⚡ {demoConfirmed ? 'Action Executed' : 'Action Proposal'}</strong>
                            <Badge variant={demoConfirmed ? 'success' : 'warning'}>
                              {demoConfirmed ? 'EXECUTED' : 'PENDING'}
                            </Badge>
                          </div>
                          <p className="dm-demo-result__summary">{activeDemo.proposal.summary}</p>
                          {!demoConfirmed ? (
                            <div className="dm-demo-result__actions">
                              <Button
                                variant="success"
                                size="sm"
                                onClick={() => setDemoConfirmed(true)}
                              >
                                Confirm Action
                              </Button>
                              <Button variant="ghost" size="sm">
                                Cancel
                              </Button>
                            </div>
                          ) : (
                            <div className="dm-demo-result__executed-msg">
                              ✅ 2 reminders scheduled successfully in your notification feed.
                            </div>
                          )}
                        </div>
                      )}

                      {activeDemo.responseType === 'provider' && (
                        <div className="dm-demo-result dm-demo-result--provider">
                          <div className="dm-demo-result__header">
                            <strong>{activeDemo.provider.name}</strong>
                            <TrustBadge type="verified" />
                          </div>
                          <span className="dm-demo-result__service">{activeDemo.provider.service}</span>
                          <div className="dm-demo-result__meta">
                            <span>{activeDemo.provider.rating}</span>
                            <span>·</span>
                            <span>{activeDemo.provider.location}</span>
                          </div>
                          <div className="dm-demo-result__footer">
                            <span className="dm-demo-result__price">{activeDemo.provider.price}</span>
                            <Button variant="primary" size="sm" onClick={() => handleCtaClick('demo_provider')}>
                              Contact Provider
                            </Button>
                          </div>
                        </div>
                      )}

                      {activeDemo.responseType === 'blood' && (
                        <div className="dm-demo-result dm-demo-result--blood">
                          <div className="dm-demo-result__header">
                            <Badge variant="danger">{activeDemo.request.urgency}</Badge>
                            <span className="dm-demo-result__bg">{activeDemo.request.bloodGroup}</span>
                          </div>
                          <strong>Patient: {activeDemo.request.patient}</strong>
                          <p className="dm-demo-result__hosp">📍 {activeDemo.request.hospital}</p>
                          <Button variant="danger" size="sm" onClick={() => handleCtaClick('demo_blood')}>
                            Respond to Appeal
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </ResponsiveContainer>
        </section>

        {/* ===================================================================
            CORE PROBLEMS & SOLUTION VALUE
           =================================================================== */}
        <section id="features" className="dm-landing-features">
          <ResponsiveContainer size="dashboard">
            <div className="dm-section-heading">
              <span className="dm-section-eyebrow">Connected Product Ecosystem</span>
              <h2 className="dm-section-title">Built for how real life actually happens</h2>
              <p className="dm-section-desc">
                Stop switching between 6 different apps. DailyMate unifies personal routines, community support, and local services into one coordinated system.
              </p>
            </div>

            <div className="dm-features-grid">
              <Card className="dm-feature-card dm-feature-card--expense">
                <span className="dm-feature-card__icon">💰</span>
                <h3>Understand & Control Spending</h3>
                <p>
                  Log expenses in seconds, track monthly budget health, inspect category breakdowns, and receive AI-driven spending optimization recommendations.
                </p>
                <div className="dm-feature-card__tag">Personal Finance</div>
              </Card>

              <Card className="dm-feature-card dm-feature-card--health">
                <span className="dm-feature-card__icon">💊</span>
                <h3>Never Miss Medication</h3>
                <p>
                  Personal medication schedules, smart dosage alarms, adherence scoring, and emergency ICE contacts accessible in one tap.
                </p>
                <div className="dm-feature-card__tag">Health & Routines</div>
              </Card>

              <Card className="dm-feature-card dm-feature-card--community">
                <span className="dm-feature-card__icon">🤝</span>
                <h3>Community Support & Civic Alerts</h3>
                <p>
                  Broadcast urgent blood donation requests, report civic complaints (potholes, street lights), track lost & found belongings, and discover local events.
                </p>
                <div className="dm-feature-card__tag">Community Hub</div>
              </Card>

              <Card className="dm-feature-card dm-feature-card--marketplace">
                <span className="dm-feature-card__icon">🛠️</span>
                <h3>Verified Service Marketplace</h3>
                <p>
                  Find trusted neighborhood professionals — plumbers, electricians, tutors, cleaners — with verified ratings and transparent upfront rates.
                </p>
                <div className="dm-feature-card__tag">Local Services</div>
              </Card>

              <Card className="dm-feature-card dm-feature-card--ai">
                <span className="dm-feature-card__icon">✨</span>
                <h3>Autonomous AI Life Assistant</h3>
                <p>
                  Type or speak in natural language. Process cross-domain bulk actions with explicit confirmation before changes are made to your account.
                </p>
                <div className="dm-feature-card__tag">Intelligence</div>
              </Card>

              <Card className="dm-feature-card dm-feature-card--security">
                <span className="dm-feature-card__icon">🔒</span>
                <h3>Trust & Zero Silent Mutations</h3>
                <p>
                  Strict action lifecycle states (PENDING → EXECUTED). We never silently modify your data, and your records remain encrypted and private.
                </p>
                <div className="dm-feature-card__tag">Security & Trust</div>
              </Card>
            </div>
          </ResponsiveContainer>
        </section>

        {/* ===================================================================
            MARKETPLACE & PROVIDER MONETIZATION STORY
           =================================================================== */}
        <section className="dm-landing-marketplace">
          <ResponsiveContainer size="dashboard">
            <div className="dm-marketplace-box">
              <div className="dm-marketplace-box__copy">
                <Badge domain="marketplace">For Local Professionals</Badge>
                <h2>Grow your business with verified community leads</h2>
                <p>
                  Are you a plumber, tutor, electrician, healthcare assistant, or cleaner? Join DailyMate to get discovered by verified local residents needing your expertise.
                </p>
                <ul className="dm-marketplace-box__perks">
                  <li>✓ Direct inquiries without hidden commissions</li>
                  <li>✓ Verified Provider badge to build immediate customer trust</li>
                  <li>✓ Instant location-based matching with community requests</li>
                </ul>
                <Button variant="primary" onClick={() => handleCtaClick('marketplace_provider')}>
                  Become a Verified Provider
                </Button>
              </div>

              <div className="dm-marketplace-box__stat">
                <StatCard
                  domain="marketplace"
                  label="Local Reach"
                  value="100% Verified"
                  trend="0% Commission"
                  trendDirection="up"
                  trendLabel="Direct Customer Inquiries"
                  icon="⭐"
                />
              </div>
            </div>
          </ResponsiveContainer>
        </section>

        {/* ===================================================================
            TRANSPARENT PRICING TIERS
           =================================================================== */}
        <section id="pricing" className="dm-landing-pricing">
          <ResponsiveContainer size="dashboard">
            <div className="dm-section-heading">
              <span className="dm-section-eyebrow">Fair & Simple Pricing</span>
              <h2 className="dm-section-title">Start free. Upgrade as your life expands.</h2>
              <p className="dm-section-desc">
                No hidden fees. Full access to personal routines and community safety.
              </p>
            </div>

            <div className="dm-pricing-grid">
              {/* Free Plan */}
              <div className="dm-pricing-card">
                <div className="dm-pricing-card__header">
                  <h3>Free Essential</h3>
                  <p>For individuals organizing daily routines and community needs.</p>
                  <div className="dm-pricing-card__price">
                    <strong>₹0</strong>
                    <span>/ forever</span>
                  </div>
                </div>
                <ul className="dm-pricing-card__features">
                  <li>✓ Expense tracking & budget limits</li>
                  <li>✓ Medicine reminders & schedule</li>
                  <li>✓ Emergency ICE contacts & hotlines</li>
                  <li>✓ Blood donation appeals & community posts</li>
                  <li>✓ Standard AI assistant questions</li>
                </ul>
                <Button variant="outline" fullWidth onClick={() => handleCtaClick('pricing_free')}>
                  Get Started Free
                </Button>
              </div>

              {/* Premium Assistant Plan */}
              <div className="dm-pricing-card dm-pricing-card--featured">
                <div className="dm-pricing-card__popular-badge">Most Popular</div>
                <div className="dm-pricing-card__header">
                  <h3>Life Assistant Pro</h3>
                  <p>Advanced autonomous AI assistance, multi-domain automations, and reports.</p>
                  <div className="dm-pricing-card__price">
                    <strong>₹199</strong>
                    <span>/ month</span>
                  </div>
                </div>
                <ul className="dm-pricing-card__features">
                  <li>✓ Everything in Free Essential</li>
                  <li>✓ Autonomous cross-domain bulk processing</li>
                  <li>✓ Deep monthly financial & life analytics reports</li>
                  <li>✓ Priority notification dispatch</li>
                  <li>✓ Unlimited medicine schedules & family profiles</li>
                </ul>
                <Button variant="primary" fullWidth onClick={() => handleCtaClick('pricing_pro')}>
                  Start 14-Day Free Trial
                </Button>
              </div>

              {/* Verified Provider Plan */}
              <div className="dm-pricing-card">
                <div className="dm-pricing-card__header">
                  <h3>Verified Provider</h3>
                  <p>For local service providers and verified businesses.</p>
                  <div className="dm-pricing-card__price">
                    <strong>₹499</strong>
                    <span>/ month</span>
                  </div>
                </div>
                <ul className="dm-pricing-card__features">
                  <li>✓ Verified Partner Badge</li>
                  <li>✓ Priority directory placement in Marketplace</li>
                  <li>✓ Direct customer inquiry inbox</li>
                  <li>✓ Rating & testimonial showcase</li>
                  <li>✓ Lead analytics dashboard</li>
                </ul>
                <Button variant="outline" fullWidth onClick={() => handleCtaClick('pricing_provider')}>
                  Join as Provider
                </Button>
              </div>
            </div>
          </ResponsiveContainer>
        </section>

        {/* ===================================================================
            FAQ ACCORDION
           =================================================================== */}
        <section className="dm-landing-faq">
          <ResponsiveContainer size="content">
            <div className="dm-section-heading">
              <span className="dm-section-eyebrow">Frequently Asked Questions</span>
              <h2 className="dm-section-title">Answers to common questions</h2>
            </div>

            <div className="dm-faq-accordion">
              {FAQS.map((faq, index) => (
                <div
                  key={index}
                  className={`dm-faq-item ${openFaqIndex === index ? 'open' : ''}`}
                >
                  <button
                    type="button"
                    className="dm-faq-question"
                    onClick={() => toggleFaq(index)}
                    aria-expanded={openFaqIndex === index}
                  >
                    <span>{faq.q}</span>
                    <span className="dm-faq-chevron">{openFaqIndex === index ? '−' : '+'}</span>
                  </button>
                  {openFaqIndex === index && (
                    <div className="dm-faq-answer">
                      <p>{faq.a}</p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </ResponsiveContainer>
        </section>

        {/* ===================================================================
            FINAL CALL TO ACTION BANNER
           =================================================================== */}
        <section className="dm-landing-cta-banner">
          <ResponsiveContainer size="dashboard">
            <div className="dm-cta-box">
              <h2>Ready to simplify your everyday life?</h2>
              <p>Join thousands of members managing expenses, health reminders, and community services in one trusted place.</p>
              <div className="dm-cta-box__actions">
                <Button variant="primary" size="lg" onClick={() => handleCtaClick('final_banner')}>
                  Create Your Free Account
                </Button>
                <Link to="/login">
                  <Button variant="outline" size="lg">
                    Sign In to Existing Account
                  </Button>
                </Link>
              </div>
            </div>
          </ResponsiveContainer>
        </section>
      </div>
    </Layout>
  )
}
