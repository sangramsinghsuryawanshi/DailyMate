import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import MainLayout from '../../layouts/MainLayout'
import { useAuth } from '../../hooks/useAuth'
import {
  getAdminStats,
  getAdminComplaints,
  updateComplaintStatus,
  getAdminJobs,
  updateAdminJobStatus,
  deleteAdminJob,
  getAdminBloodRequests,
  updateAdminBloodRequestStatus,
  deleteAdminBloodRequest,
  getAdminEvents,
  updateAdminEventStatus,
  deleteAdminEvent,
  getAdminLostFound,
  deleteAdminLostFound,
  getAdminUsers,
  updateAdminUserStatus,
} from '../services/adminApi'
import { trackEvent, AnalyticsEvents } from '../../analytics/tracker'
import AdminStats from '../components/AdminStats'
import AdminTabs from '../components/AdminTabs'
import ComplaintsModeration from '../components/ComplaintsModeration'
import JobsModeration from '../components/JobsModeration'
import BloodRequestsModeration from '../components/BloodRequestsModeration'
import EventsModeration from '../components/EventsModeration'
import LostFoundModeration from '../components/LostFoundModeration'
import UsersModeration from '../components/UsersModeration'
import { Button, ResponsiveContainer, Card } from '../../design-system'
import './AdminPage.css'

export default function AdminPage() {
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const [activeTab, setActiveTab] = useState('overview')
  const [errorMsg, setErrorMsg] = useState('')

  useEffect(() => {
    trackEvent(AnalyticsEvents.PAGE_VIEW, { page: 'admin' })
  }, [])

  // 1. Role Guard
  const isAdmin = user?.role === 'ADMIN'

  // 2. Data Queries
  const { data: stats, isLoading: statsLoading, isError: statsError } = useQuery({
    queryKey: ['admin-stats'],
    queryFn: getAdminStats,
    enabled: isAdmin,
  })

  const { data: complaints = [], isLoading: complaintsLoading } = useQuery({
    queryKey: ['admin-complaints'],
    queryFn: getAdminComplaints,
    enabled: isAdmin,
  })

  const { data: jobs = [], isLoading: jobsLoading } = useQuery({
    queryKey: ['admin-jobs'],
    queryFn: getAdminJobs,
    enabled: isAdmin,
  })

  const { data: bloodRequests = [], isLoading: bloodLoading } = useQuery({
    queryKey: ['admin-blood-requests'],
    queryFn: getAdminBloodRequests,
    enabled: isAdmin,
  })

  const { data: events = [], isLoading: eventsLoading } = useQuery({
    queryKey: ['admin-events'],
    queryFn: getAdminEvents,
    enabled: isAdmin,
  })

  const { data: lostFound = [], isLoading: lostFoundLoading } = useQuery({
    queryKey: ['admin-lost-found'],
    queryFn: getAdminLostFound,
    enabled: isAdmin,
  })

  const { data: users = [], isLoading: usersLoading } = useQuery({
    queryKey: ['admin-users'],
    queryFn: getAdminUsers,
    enabled: isAdmin,
  })

  // 3. Mutations with precise React Query Invalidation
  const complaintMutation = useMutation({
    mutationFn: ({ id, status }) => updateComplaintStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-complaints'] })
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] })
      queryClient.invalidateQueries({ queryKey: ['community-complaints'] })
      setErrorMsg('')
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to update complaint status.')
    },
  })

  const jobStatusMutation = useMutation({
    mutationFn: ({ id, status }) => updateAdminJobStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-jobs'] })
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] })
      queryClient.invalidateQueries({ queryKey: ['jobs'] })
      setErrorMsg('')
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to update job status.')
    },
  })

  const jobDeleteMutation = useMutation({
    mutationFn: (id) => deleteAdminJob(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-jobs'] })
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] })
      queryClient.invalidateQueries({ queryKey: ['jobs'] })
      setErrorMsg('')
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to delete job.')
    },
  })

  const bloodStatusMutation = useMutation({
    mutationFn: ({ id, status }) => updateAdminBloodRequestStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-blood-requests'] })
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] })
      queryClient.invalidateQueries({ queryKey: ['blood-requests'] })
      setErrorMsg('')
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to update blood request status.')
    },
  })

  const bloodDeleteMutation = useMutation({
    mutationFn: (id) => deleteAdminBloodRequest(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-blood-requests'] })
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] })
      queryClient.invalidateQueries({ queryKey: ['blood-requests'] })
      setErrorMsg('')
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to delete blood request.')
    },
  })

  const eventStatusMutation = useMutation({
    mutationFn: ({ id, status }) => updateAdminEventStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-events'] })
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] })
      queryClient.invalidateQueries({ queryKey: ['local-events'] })
      setErrorMsg('')
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to update event status.')
    },
  })

  const eventDeleteMutation = useMutation({
    mutationFn: (id) => deleteAdminEvent(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-events'] })
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] })
      queryClient.invalidateQueries({ queryKey: ['local-events'] })
      setErrorMsg('')
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to delete event.')
    },
  })

  const lostFoundDeleteMutation = useMutation({
    mutationFn: (id) => deleteAdminLostFound(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-lost-found'] })
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] })
      queryClient.invalidateQueries({ queryKey: ['lost-found-posts'] })
      setErrorMsg('')
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to delete lost item post.')
    },
  })

  const userStatusMutation = useMutation({
    mutationFn: ({ id, status }) => updateAdminUserStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] })
      setErrorMsg('')
    },
    onError: (err) => {
      setErrorMsg(err.response?.data?.detail || err.response?.data?.message || 'Failed to update user account status.')
    },
  })

  // 4. Non-Admin Access Guard
  if (!isAdmin) {
    return (
      <MainLayout>
        <div className="dm-admin-page">
          <ResponsiveContainer size="narrow">
            <div className="dm-empty-dashboard-box" style={{ padding: '3rem 2rem', textAlign: 'center' }}>
              <span className="dm-section-eyebrow">Access Denied</span>
              <h1 className="dm-page-main-title" style={{ fontSize: '1.5rem', margin: '0.5rem 0' }}>
                Administrator Privileges Required
              </h1>
              <p style={{ color: 'var(--dm-color-text-soft)', marginBottom: '1.5rem' }}>
                Your current user account does not have permission to view or manage administrative operations.
              </p>
              <Link to="/dashboard">
                <Button variant="primary">Return to Dashboard</Button>
              </Link>
            </div>
          </ResponsiveContainer>
        </div>
      </MainLayout>
    )
  }

  const counts = {
    complaints: complaints.length,
    jobs: jobs.length,
    blood: bloodRequests.length,
    events: events.length,
    lostFound: lostFound.length,
    users: users.length,
  }

  return (
    <MainLayout>
      <div className="dm-admin-page">
        <ResponsiveContainer size="wide">
          {/* Header */}
          <div className="dm-page-header-row">
            <div>
              <span className="dm-section-eyebrow">Administration & Governance</span>
              <h1 className="dm-page-main-title">Community Admin Moderation Hub</h1>
              <p className="dm-page-subtitle">
                Cross-module supervision, real-time community statistics, content moderation, and user account management.
              </p>
            </div>
            <div className="dm-page-header-actions">
              <Link to="/dashboard">
                <Button variant="ghost" size="md">
                  Back to Dashboard
                </Button>
              </Link>
            </div>
          </div>

          {errorMsg && (
            <div className="dm-form-alert dm-form-alert--error" style={{ marginBottom: '1.5rem' }}>
              <strong>Error:</strong> {errorMsg}
            </div>
          )}

          {/* Database-backed metrics stats overview */}
          <AdminStats stats={stats} isLoading={statsLoading} isError={statsError} />

          <div style={{ marginTop: '1.5rem' }}>
            <AdminTabs activeTab={activeTab} onSelectTab={setActiveTab} counts={counts} />

            <section className="dm-admin-feed-list" style={{ marginTop: '1rem' }}>
              {activeTab === 'overview' && (
                <div className="dm-admin-item-card">
                  <div className="dm-admin-item-header">
                    <h2 style={{ fontSize: '1.25rem', margin: 0 }}>Administrative Summary</h2>
                  </div>
                  <p style={{ color: 'var(--dm-color-text-soft)', margin: '0.25rem 0 1rem 0' }}>
                    Select a tab above to moderate submitted complaints, job postings, emergency blood requests, community events, lost items, or manage user accounts.
                  </p>
                  <div>
                    <h3 style={{ fontSize: '1rem', margin: '0 0 0.5rem 0' }}>Active Moderation Queues</h3>
                    <ul style={{ paddingLeft: '1.25rem', margin: 0, lineHeight: '1.75', color: 'var(--dm-color-text)' }}>
                      <li><strong>{stats?.openComplaints ?? 0}</strong> complaints pending review</li>
                      <li><strong>{stats?.openJobs ?? 0}</strong> active job postings</li>
                      <li><strong>{stats?.openBloodRequests ?? 0}</strong> active blood requests</li>
                      <li><strong>{stats?.publishedEvents ?? 0}</strong> published events</li>
                      <li><strong>{stats?.suspendedUsers ?? 0}</strong> suspended user accounts</li>
                    </ul>
                  </div>
                </div>
              )}

              {activeTab === 'complaints' && (
                <div>
                  <h2 style={{ fontSize: '1.25rem', marginBottom: '1rem' }}>Complaints Moderation ({complaints.length})</h2>
                  {complaintsLoading ? (
                    <p>Loading complaints…</p>
                  ) : (
                    <ComplaintsModeration
                      complaints={complaints}
                      onUpdateStatus={(id, status) => complaintMutation.mutate({ id, status })}
                      isUpdating={complaintMutation.isPending}
                    />
                  )}
                </div>
              )}

              {activeTab === 'jobs' && (
                <div>
                  <h2 style={{ fontSize: '1.25rem', marginBottom: '1rem' }}>Job Postings Moderation ({jobs.length})</h2>
                  {jobsLoading ? (
                    <p>Loading job postings…</p>
                  ) : (
                    <JobsModeration
                      jobs={jobs}
                      onUpdateStatus={(id, status) => jobStatusMutation.mutate({ id, status })}
                      onDeleteJob={(id) => jobDeleteMutation.mutate(id)}
                      isMutating={jobStatusMutation.isPending || jobDeleteMutation.isPending}
                    />
                  )}
                </div>
              )}

              {activeTab === 'blood' && (
                <div>
                  <h2 style={{ fontSize: '1.25rem', marginBottom: '1rem' }}>Blood Requests Moderation ({bloodRequests.length})</h2>
                  {bloodLoading ? (
                    <p>Loading blood requests…</p>
                  ) : (
                    <BloodRequestsModeration
                      requests={bloodRequests}
                      onUpdateStatus={(id, status) => bloodStatusMutation.mutate({ id, status })}
                      onDeleteRequest={(id) => bloodDeleteMutation.mutate(id)}
                      isMutating={bloodStatusMutation.isPending || bloodDeleteMutation.isPending}
                    />
                  )}
                </div>
              )}

              {activeTab === 'events' && (
                <div>
                  <h2 style={{ fontSize: '1.25rem', marginBottom: '1rem' }}>Local Events Moderation ({events.length})</h2>
                  {eventsLoading ? (
                    <p>Loading events…</p>
                  ) : (
                    <EventsModeration
                      events={events}
                      onUpdateStatus={(id, status) => eventStatusMutation.mutate({ id, status })}
                      onDeleteEvent={(id) => eventDeleteMutation.mutate(id)}
                      isMutating={eventStatusMutation.isPending || eventDeleteMutation.isPending}
                    />
                  )}
                </div>
              )}

              {activeTab === 'lost-found' && (
                <div>
                  <h2 style={{ fontSize: '1.25rem', marginBottom: '1rem' }}>Lost & Found Moderation ({lostFound.length})</h2>
                  {lostFoundLoading ? (
                    <p>Loading listings…</p>
                  ) : (
                    <LostFoundModeration
                      items={lostFound}
                      onDeleteItem={(id) => lostFoundDeleteMutation.mutate(id)}
                      isMutating={lostFoundDeleteMutation.isPending}
                    />
                  )}
                </div>
              )}

              {activeTab === 'users' && (
                <div>
                  <h2 style={{ fontSize: '1.25rem', marginBottom: '1rem' }}>User Account Management ({users.length})</h2>
                  {usersLoading ? (
                    <p>Loading user accounts…</p>
                  ) : (
                    <UsersModeration
                      users={users}
                      currentUser={user}
                      onUpdateStatus={(id, status) => userStatusMutation.mutate({ id, status })}
                      isMutating={userStatusMutation.isPending}
                    />
                  )}
                </div>
              )}
            </section>
          </div>
        </ResponsiveContainer>
      </div>
    </MainLayout>
  )
}
