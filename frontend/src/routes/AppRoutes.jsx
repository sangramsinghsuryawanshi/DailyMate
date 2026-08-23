import { Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from '../auth/pages/LoginPage'
import RegisterPage from '../auth/pages/RegisterPage'
import ForgotPasswordPage from '../auth/pages/ForgotPasswordPage'
import OnboardingPage from '../features/onboarding/OnboardingPage'
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
import DesignSystemShowcasePage from '../design-system/showcase/DesignSystemShowcasePage'
import LandingPage from '../features/landing/LandingPage'
import AboutPage from '../features/about/AboutPage'
import PricingPage from '../features/public/PricingPage'
import HowItWorksPage from '../features/public/HowItWorksPage'
import FaqPage from '../features/public/FaqPage'
import ContactPage from '../features/public/ContactPage'
import PrivacyPage from '../features/public/PrivacyPage'
import TermsPage from '../features/public/TermsPage'
import ProtectedRoute from './ProtectedRoute'

export default function AppRoutes() {
  return (
    <Routes>
      {/* Shared Public Routes (PublicLayout) */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/about" element={<AboutPage />} />
      <Route path="/pricing" element={<PricingPage />} />
      <Route path="/how-it-works" element={<HowItWorksPage />} />
      <Route path="/faq" element={<FaqPage />} />
      <Route path="/contact" element={<ContactPage />} />
      <Route path="/privacy" element={<PrivacyPage />} />
      <Route path="/terms" element={<TermsPage />} />
      <Route path="/design-system" element={<DesignSystemShowcasePage />} />

      {/* Auth Routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/onboarding" element={<OnboardingPage />} />

      {/* Domain Feature Routes */}
      <Route path="/marketplace" element={<MarketplacePage />} />
      <Route path="/marketplace/:id" element={<ProviderDetailPage />} />
      <Route path="/blood" element={<BloodDonationPage />} />
      <Route path="/emergency-contacts" element={<EmergencyContactsPage />} />
      <Route path="/community-complaints" element={<CommunityComplaintsPage />} />
      <Route path="/events" element={<LocalEventsPage />} />
      <Route path="/jobs" element={<JobsPage />} />
      <Route path="/grocery" element={<GroceryPage />} />
      <Route path="/lost-found" element={<LostFoundPage />} />

      {/* Protected Routes (MainLayout) */}
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
