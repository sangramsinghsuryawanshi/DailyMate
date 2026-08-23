import AppRoutes from './routes/AppRoutes'
import { ToastProvider } from './design-system'
import { FeatureFlagProvider } from './feature-flags/flags.jsx'
import ErrorBoundary from './components/ErrorBoundary'

export default function App() {
  return (
    <ErrorBoundary>
      <FeatureFlagProvider>
        <ToastProvider>
          <AppRoutes />
        </ToastProvider>
      </FeatureFlagProvider>
    </ErrorBoundary>
  )
}

