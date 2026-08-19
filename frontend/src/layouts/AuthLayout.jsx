export default function AuthLayout({ children, headline, subheadline }) {
  return (
    <main className="auth-shell">
      <section className="auth-spotlight">
        <div className="brand-lockup">DailyMate</div>
        <div className="spotlight-copy">
          <p className="eyebrow">Everyday life, made easier</p>
          <h1>{headline}</h1>
          <p>{subheadline}</p>
        </div>

        <div className="spotlight-points" aria-label="DailyMate benefits">
          <div>
            <strong>Smart routines</strong>
            <span>Medicine, spending, and tasks in one place.</span>
          </div>
          <div>
            <strong>Local help</strong>
            <span>Find trusted community services near you.</span>
          </div>
          <div>
            <strong>Peace of mind</strong>
            <span>Stay on top of reminders, alerts, and updates.</span>
          </div>
        </div>
      </section>

      <section className="auth-panel">{children}</section>
    </main>
  )
}
