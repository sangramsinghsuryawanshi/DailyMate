import React from 'react'
import { ErrorState } from '../design-system'

export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    console.error('[ErrorBoundary caught error]', error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
    window.location.href = '/dashboard'
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', padding: '2rem' }}>
          <ErrorState
            title="Application Error"
            message="An unexpected error occurred. Please refresh or return to the dashboard."
            onRetry={this.handleReset}
            retryLabel="Reload Dashboard"
          />
        </div>
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary
