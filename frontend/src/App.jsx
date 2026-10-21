import React from 'react'
import { Routes, Route } from 'react-router-dom'
import { ToastProvider } from './components/common/toast'
import ProtectedRoute from './routes/protected'

import PublicLayout from './layouts/public'
import UserLayout from './layouts/user'
import AdminLayout from './layouts/admin'

import Landing from './pages/public/home'
import Login from './pages/public/login'
import Register from './pages/public/register'
import ForgotPassword from './pages/public/forgot-password'
import ResetPassword from './pages/public/reset-password'
import VerifyEmail from './pages/public/verify-email'

import Dashboard from './pages/user/dashboard'
import Profile from './pages/user/profile'
import EditProfile from './pages/user/edit-profile'
import MySkills from './pages/user/skills'
import Availability from './pages/user/availability'
import FindPeople from './pages/user/discover'
import Matches from './pages/user/matches'
import PartnerVideo from './pages/user/partner-video'
import ExchangeRequests from './pages/user/exchanges'
import Chat from './pages/user/chat'
import Sessions from './pages/user/sessions'
import Wallet from './pages/user/wallet'
import Ratings from './pages/user/ratings'
import Notifications from './pages/user/notifications'
import SafetySupport from './pages/user/safety-support'

import AdminDashboard from './pages/admin/dashboard'
import UserManagement from './pages/admin/users'
import SkillManagement from './pages/admin/skills'
import ReportManagement from './pages/admin/reports'

export default function App() {
  return (
    <ToastProvider>
      <Routes>
        {/* Public */}
        <Route element={<PublicLayout />}>
          <Route path="/" element={<Landing />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/verify-email" element={<VerifyEmail />} />
        </Route>

        {/* Authenticated user area */}
        <Route element={<ProtectedRoute userOnly><UserLayout /></ProtectedRoute>}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/profile/:id" element={<Profile />} />
          <Route path="/profile/edit" element={<EditProfile />} />
          <Route path="/my-skills" element={<MySkills />} />
          <Route path="/availability" element={<Availability />} />
          <Route path="/find-people" element={<FindPeople />} />
          <Route path="/matches" element={<Matches />} />
          <Route path="/partner-video/:roomId" element={<PartnerVideo />} />
          <Route path="/exchange-requests" element={<ExchangeRequests />} />
          <Route path="/exchange-requests/new" element={<ExchangeRequests />} />
          <Route path="/chat" element={<Chat />} />
          <Route path="/chat/:userId" element={<Chat />} />
          <Route path="/sessions" element={<Sessions />} />
          <Route path="/wallet" element={<Wallet />} />
          <Route path="/ratings" element={<Ratings />} />
          <Route path="/notifications" element={<Notifications />} />
          <Route path="/support" element={<SafetySupport />} />
        </Route>

        {/* Admin area */}
        <Route element={<ProtectedRoute adminOnly><AdminLayout /></ProtectedRoute>}>
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/admin/users" element={<UserManagement />} />
          <Route path="/admin/skills" element={<SkillManagement />} />
          <Route path="/admin/reports" element={<ReportManagement />} />
        </Route>

        <Route path="*" element={<Landing />} />
      </Routes>
    </ToastProvider>
  )
}
