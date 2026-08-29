import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input, Select, Card, Badge, ResponsiveContainer } from '../../design-system'
import { useAuth } from '../../hooks/useAuth'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import './OnboardingPage.css'

const GOAL_OPTIONS = [
  {
    id: 'finance',
    icon: '💰',
    title: 'Personal Finances & Expenses',
    desc: 'Log daily spending, analyze trends, and stay within monthly budgets.',
    domain: 'expense',
  },
  {
    id: 'health',
    icon: '💊',
    title: 'Medicine & Healthcare Routines',
    desc: 'Never miss medication dosages, times, and adherence tracking.',
    domain: 'health',
  },
  {
    id: 'community',
    icon: '🤝',
    title: 'Community Safety & Civic Alerts',
    desc: 'Respond to urgent blood requests, report complaints, and discover local events.',
    domain: 'community',
  },
  {
    id: 'services',
    icon: '🛠️',
    title: 'Local Services & Marketplace',
    desc: 'Find and book verified neighborhood plumbers, tutors, electricians, and helpers.',
    domain: 'marketplace',
  },
]

export default function OnboardingPage() {
  const navigate = useNavigate()
  const { user } = useAuth()

  const [step, setStep] = useState(1)
  const [selectedGoal, setSelectedGoal] = useState('finance')

  // Step 2 profile fields
  const [phone, setPhone] = useState('')
  const [city, setCity] = useState('')
  const [emergencyContact, setEmergencyContact] = useState('')

  // Step 3 micro-action fields
  const [expenseTitle, setExpenseTitle] = useState('Lunch')
  const [expenseAmount, setExpenseAmount] = useState('50')
  const [medicineName, setMedicineName] = useState('Vitamin D')
  const [medicineTime, setMedicineTime] = useState('09:00')
  const [bloodGroup, setBloodGroup] = useState('O+')

  const handleGoalSelect = (goalId) => {
    setSelectedGoal(goalId)
  }

  const handleNext = () => {
    if (step < 3) {
      setStep(step + 1)
    } else {
      handleComplete()
    }
  }

  const handleSkip = () => {
    handleComplete()
  }

  const handleComplete = () => {
    const personalizationData = {
      primaryGoal: selectedGoal,
      phone,
      city,
      emergencyContact,
      firstAction: {
        goal: selectedGoal,
        expense: selectedGoal === 'finance' ? { title: expenseTitle, amount: expenseAmount } : null,
        medicine: selectedGoal === 'health' ? { name: medicineName, time: medicineTime } : null,
        community: selectedGoal === 'community' ? { bloodGroup } : null,
      },
      completedAt: new Date().toISOString(),
    }

    try {
      localStorage.setItem('dailymate.personalization', JSON.stringify(personalizationData))
    } catch (_) {}

    trackEvent(AnalyticsEvents.ONBOARDING_COMPLETED, { primaryGoal: selectedGoal })
    navigate('/dashboard')
  }

  return (
    <div className="dm-onboarding-shell">
      <header className="dm-onboarding-header">
        <div className="dm-onboarding-brand">
          <img src="/images/DailyMateIcon.png" alt="DailyMate" className="dm-onboarding-logo" />
          <span>DailyMate</span>
        </div>
        <button type="button" className="dm-onboarding-skip-btn" onClick={handleSkip}>
          Skip to Dashboard →
        </button>
      </header>

      <main className="dm-onboarding-main">
        <ResponsiveContainer size="form">
          {/* Progress Indicator */}
          <div className="dm-onboarding-progress">
            <div className="dm-onboarding-steps">
              <span className={`dm-step-pill ${step >= 1 ? 'active' : ''}`}>1. Priorities</span>
              <span className={`dm-step-pill ${step >= 2 ? 'active' : ''}`}>2. Profile</span>
              <span className={`dm-step-pill ${step >= 3 ? 'active' : ''}`}>3. Quick Setup</span>
            </div>
            <div className="dm-progress-bar">
              <div
                className="dm-progress-fill"
                style={{ width: step === 1 ? '33%' : step === 2 ? '66%' : '100%' }}
              />
            </div>
          </div>

          {/* ===================================================================
              STEP 1: PRIMARY LIFE PRIORITIES
             =================================================================== */}
          {step === 1 && (
            <div className="dm-onboarding-step">
              <div className="dm-onboarding-step__header">
                <h2>Welcome{user?.firstName ? `, ${user.firstName}` : ''}! What is your main focus today?</h2>
                <p>We will tailor your DailyMate command center around what matters most to you.</p>
              </div>

              <div className="dm-goal-selector-grid">
                {GOAL_OPTIONS.map((goal) => {
                  const isSelected = selectedGoal === goal.id
                  return (
                    <div
                      key={goal.id}
                      className={`dm-goal-option dm-goal-option--${goal.domain} ${isSelected ? 'selected' : ''}`}
                      onClick={() => handleGoalSelect(goal.id)}
                      role="radio"
                      aria-checked={isSelected}
                      tabIndex={0}
                    >
                      <span className="dm-goal-option__icon">{goal.icon}</span>
                      <div className="dm-goal-option__content">
                        <strong>{goal.title}</strong>
                        <span>{goal.desc}</span>
                      </div>
                      <div className="dm-goal-option__radio">
                        {isSelected && <span className="dm-radio-check">✓</span>}
                      </div>
                    </div>
                  )
                })}
              </div>

              <div className="dm-onboarding-actions">
                <Button variant="primary" size="lg" fullWidth onClick={handleNext}>
                  Continue →
                </Button>
              </div>
            </div>
          )}

          {/* ===================================================================
              STEP 2: ESSENTIAL PROFILE DATA
             =================================================================== */}
          {step === 2 && (
            <div className="dm-onboarding-step">
              <div className="dm-onboarding-step__header">
                <h2>A few quick details</h2>
                <p>Help DailyMate connect you with local alerts and emergency hotlines.</p>
              </div>

              <div className="dm-onboarding-form-fields">
                <Input
                  id="onboarding-phone"
                  label="Phone Number"
                  type="tel"
                  placeholder="+91 98765 43210"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  helperText="Used for urgent blood appeals and emergency contacts."
                />

                <Input
                  id="onboarding-city"
                  label="City / Neighborhood"
                  placeholder="e.g. Kothrud, Pune"
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  helperText="Localizes community events and marketplace providers."
                />

                <Input
                  id="onboarding-ice"
                  label="Emergency ICE Contact (Optional)"
                  placeholder="e.g. Father: 9876543210"
                  value={emergencyContact}
                  onChange={(e) => setEmergencyContact(e.target.value)}
                />
              </div>

              <div className="dm-onboarding-actions">
                <Button variant="ghost" onClick={() => setStep(1)}>
                  Back
                </Button>
                <Button variant="primary" size="lg" onClick={handleNext}>
                  Continue →
                </Button>
              </div>
            </div>
          )}

          {/* ===================================================================
              STEP 3: FIRST USEFUL MICRO-ACTION
             =================================================================== */}
          {step === 3 && (
            <div className="dm-onboarding-step">
              <div className="dm-onboarding-step__header">
                <h2>Let's set up your first item</h2>
                <p>Start with one quick entry to experience how DailyMate works.</p>
              </div>

              {selectedGoal === 'finance' && (
                <div className="dm-micro-action-card">
                  <div className="dm-micro-action-card__badge">
                    <Badge domain="expense">First Expense</Badge>
                  </div>
                  <h3>Log a recent expense</h3>
                  <div className="dm-micro-action-inputs">
                    <Input
                      label="Description"
                      value={expenseTitle}
                      onChange={(e) => setExpenseTitle(e.target.value)}
                    />
                    <Input
                      label="Amount (₹)"
                      type="number"
                      value={expenseAmount}
                      onChange={(e) => setExpenseAmount(e.target.value)}
                    />
                  </div>
                </div>
              )}

              {selectedGoal === 'health' && (
                <div className="dm-micro-action-card">
                  <div className="dm-micro-action-card__badge">
                    <Badge domain="health">First Reminder</Badge>
                  </div>
                  <h3>Schedule your daily medication</h3>
                  <div className="dm-micro-action-inputs">
                    <Input
                      label="Medicine Name"
                      value={medicineName}
                      onChange={(e) => setMedicineName(e.target.value)}
                    />
                    <Input
                      label="Time"
                      type="time"
                      value={medicineTime}
                      onChange={(e) => setMedicineTime(e.target.value)}
                    />
                  </div>
                </div>
              )}

              {(selectedGoal === 'community' || selectedGoal === 'services') && (
                <div className="dm-micro-action-card">
                  <div className="dm-micro-action-card__badge">
                    <Badge domain="community">Community Profile</Badge>
                  </div>
                  <h3>Select your blood group</h3>
                  <Select
                    label="Blood Group"
                    value={bloodGroup}
                    onChange={(e) => setBloodGroup(e.target.value)}
                    options={['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']}
                    helperText="Helps notify you if urgent community blood is needed."
                  />
                </div>
              )}

              <div className="dm-onboarding-actions">
                <Button variant="ghost" onClick={() => setStep(2)}>
                  Back
                </Button>
                <Button variant="primary" size="lg" onClick={handleComplete}>
                  Complete & Go to Dashboard 🚀
                </Button>
              </div>
            </div>
          )}
        </ResponsiveContainer>
      </main>
    </div>
  )
}
