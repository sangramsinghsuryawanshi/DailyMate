import { useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from './useAuth'
import { getProfile, updateProfile } from '../user/services/userApi'
import { domainBus, DomainEvents } from '../events/domainBus'

const PERSONALIZATION_STORAGE_KEY = 'dailymate.personalization.v1'

const DEFAULT_PERSONALIZATION = {
  primaryGoal: 'finance',
  focusAreas: ['expenses', 'medicines', 'community'],
  emergencyContact: '',
  city: 'Pune',
  version: 1,
}

export function usePersonalization() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  // 1. Fetch Authoritative User Profile from Server
  const { data: profile, isLoading: isProfileLoading } = useQuery({
    queryKey: ['profile'],
    queryFn: getProfile,
    enabled: Boolean(user?.id),
    staleTime: 5 * 60 * 1000,
  })

  // 2. Read local cache scoped by user
  const localCache = useMemo(() => {
    if (typeof window === 'undefined') return DEFAULT_PERSONALIZATION
    try {
      const userKey = user?.id ? `${PERSONALIZATION_STORAGE_KEY}.${user.id}` : PERSONALIZATION_STORAGE_KEY
      const raw = localStorage.getItem(userKey)
      return raw ? JSON.parse(raw) : DEFAULT_PERSONALIZATION
    } catch {
      return DEFAULT_PERSONALIZATION
    }
  }, [user?.id])

  // 3. Reconcile Server Profile metadata vs Local Cache
  const preferences = useMemo(() => {
    // If server profile has preferences metadata, it is authoritative
    if (profile?.preferences && typeof profile.preferences === 'object') {
      return {
        ...DEFAULT_PERSONALIZATION,
        ...profile.preferences,
      }
    }
    return localCache || DEFAULT_PERSONALIZATION
  }, [profile, localCache])

  // 4. Mutation to update preferences server-side and update local cache
  const updatePreferencesMutation = useMutation({
    mutationFn: async (newPreferences) => {
      const merged = { ...preferences, ...newPreferences, version: 1 }

      // Update local storage cache immediately for optimistic UI
      if (typeof window !== 'undefined') {
        const userKey = user?.id ? `${PERSONALIZATION_STORAGE_KEY}.${user.id}` : PERSONALIZATION_STORAGE_KEY
        localStorage.setItem(userKey, JSON.stringify(merged))
      }

      // If user is authenticated, persist to server
      if (user?.id) {
        await updateProfile({
          firstName: profile?.firstName || user.firstName,
          lastName: profile?.lastName || user.lastName,
          preferences: merged,
        })
      }

      return merged
    },
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['profile'] })
      domainBus.publish({
        type: DomainEvents.PERSONALIZATION_CHANGED,
        domain: 'USER',
        action: 'updated',
        metadata: saved,
      })
    },
  })

  return {
    preferences,
    isLoading: isProfileLoading,
    updatePreferences: updatePreferencesMutation.mutate,
    isUpdating: updatePreferencesMutation.isPending,
  }
}
