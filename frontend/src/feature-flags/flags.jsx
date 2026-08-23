import { createContext, useContext, useState, useMemo } from 'react'

/**
 * DailyMate Feature Flags
 * Controls safe rollout of capabilities without code restructuring.
 */

export const DefaultFeatureFlags = {
  FEATURE_AI_ASSISTANT: true,
  FEATURE_MARKETPLACE: true,
  FEATURE_DARK_MODE: true,
  FEATURE_QUICK_ACTIONS: true,
  FEATURE_ONBOARDING: true,
  FEATURE_PREMIUM_GATING: false,
}

const FeatureFlagContext = createContext({
  flags: DefaultFeatureFlags,
  isEnabled: (flag) => !!DefaultFeatureFlags[flag],
  setFlag: () => {},
})

export function FeatureFlagProvider({ children, initialFlags = {} }) {
  const [flags, setFlags] = useState(() => {
    try {
      const stored = localStorage.getItem('dailymate.feature_flags')
      if (stored) {
        return { ...DefaultFeatureFlags, ...JSON.parse(stored), ...initialFlags }
      }
    } catch (_) {}
    return { ...DefaultFeatureFlags, ...initialFlags }
  })

  const setFlag = (flagName, value) => {
    setFlags((prev) => {
      const updated = { ...prev, [flagName]: value }
      try {
        localStorage.setItem('dailymate.feature_flags', JSON.stringify(updated))
      } catch (_) {}
      return updated
    })
  }

  const isEnabled = (flagName) => !!flags[flagName]

  const contextValue = useMemo(() => ({ flags, isEnabled, setFlag }), [flags])

  return (
    <FeatureFlagContext.Provider value={contextValue}>
      {children}
    </FeatureFlagContext.Provider>
  )
}

export function useFeatureFlag(flagName) {
  const { isEnabled } = useContext(FeatureFlagContext)
  return isEnabled(flagName)
}

export function useFeatureFlags() {
  return useContext(FeatureFlagContext)
}
