import { Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from '../auth/pages/LoginPage'
import RegisterPage from '../auth/pages/RegisterPage'
import DashboardPage from '../user/pages/DashboardPage'
import ProfilePage from '../user/pages/ProfilePage'
import MarketplacePage from '../marketplace/pages/MarketplacePage'
import ProviderDetailPage from '../marketplace/pages/ProviderDetailPage'
import MedicinePage from '../medicine/pages/MedicinePage'
import ExpensePage from '../expense/pages/ExpensePage'
import AssistantPage from '../assistant/pages/AssistantPage'
import BloodDonationPage from '../blood/pages/BloodDonationPage'
import CommunityComplaintsPage from '../community/pages/CommunityComplaintsPage'
import EmergencyContactsPage from '../emergency/pages/EmergencyContactsPage'
import GroceryPage from '../grocery/pages/GroceryPage'
import JobsPage from '../jobs/pages/JobsPage'
import LocalEventsPage from '../events/pages/LocalEventsPage'
import LostFoundPage from '../lostfound/pages/LostFoundPage'
import NotificationsPage from '../notification/pages/NotificationsPage'
import AdminPage from '../admin/pages/AdminPage'
import ProtectedRoute from './ProtectedRoute'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route path="/marketplace" element={<MarketplacePage />} />
      <Route path="/marketplace/:id" element={<ProviderDetailPage />} />
      <Route path="/blood" element={<BloodDonationPage />} />
      <Route path="/emergency-contacts" element={<EmergencyContactsPage />} />
      <Route path="/community-complaints" element={<CommunityComplaintsPage />} />
      <Route path="/events" element={<LocalEventsPage />} />
      <Route path="/jobs" element={<JobsPage />} />
      <Route path="/grocery" element={<GroceryPage />} />
      <Route path="/lost-found" element={<LostFoundPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/medicines" element={<MedicinePage />} />
        <Route path="/expenses" element={<ExpensePage />} />
        <Route path="/assistant" element={<AssistantPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
