import { useState } from 'react'
import { Activity, Building2, CalendarDays, House, KeyRound, LayoutDashboard, LogOut, Shapes, Users } from 'lucide-react'
import { Link, Navigate, NavLink, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './auth/useAuth'
import { ForbiddenPage, LoginPage, RegisterPage } from './auth/AuthPages'
import { AdminDashboardPage, TenantDashboardPage } from './dashboard/DashboardPages'
import TenantRoomPage from './tenant/TenantRoomPage'
import HealthPage from './App'
import PropertyManagementPage from './property/PropertyManagementPage'
import { AdminBookingPage, RoomsPage } from './booking/BookingPages'
import PasswordChangePage from './auth/PasswordChangePage'
import AdminTenantPage from './tenant/AdminTenantPage'
import './auth/password-change.css'
import './app-shell.css'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<LandingRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/403" element={<ForbiddenPage />} />
      <Route path="/health" element={<HealthPage />} />
      <Route path="/rooms" element={<RoomsPage />} />
      <Route element={<RequireAuthentication />}>
        <Route element={<DashboardLayout />}>
          <Route path="/admin/dashboard" element={<RequireRole role="ADMIN"><AdminDashboardPage /></RequireRole>} />
          <Route path="/admin/khu-tro" element={<RequireRole role="ADMIN"><PropertyManagementPage key="khu-tro" kind="khu-tro" /></RequireRole>} />
          <Route path="/admin/loai-phong" element={<RequireRole role="ADMIN"><PropertyManagementPage key="loai-phong" kind="loai-phong" /></RequireRole>} />
          <Route path="/admin/phong-tro" element={<RequireRole role="ADMIN"><PropertyManagementPage key="phong-tro" kind="phong-tro" /></RequireRole>} />
          <Route path="/admin/nguoi-thue" element={<RequireRole role="ADMIN"><AdminTenantPage /></RequireRole>} />
          <Route path="/admin/dat-phong" element={<RequireRole role="ADMIN"><AdminBookingPage /></RequireRole>} />
          <Route path="/tenant/dashboard" element={<RequireRole role="NGUOI_THUE"><TenantDashboardPage /></RequireRole>} />
          <Route path="/tenant/my-room" element={<RequireRole role="NGUOI_THUE"><TenantRoomPage /></RequireRole>} />
          <Route path="/tenant/dat-phong" element={<RequireRole role="NGUOI_THUE"><RoomsPage /></RequireRole>} />
          <Route path="/account/password" element={<PasswordChangePage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

function LandingRedirect() {
  const { status, user } = useAuth()
  if (status === 'checking') return <RouteLoading />
  if (!user) return <Navigate to="/login" replace />
  return <Navigate to={user.role === 'ADMIN' ? '/admin/dashboard' : user.role === 'NGUOI_THUE' ? '/tenant/dashboard' : '/403'} replace />
}

function RequireAuthentication() {
  const { status } = useAuth()
  if (status === 'checking') return <RouteLoading />
  if (status !== 'authenticated') return <Navigate to="/login" replace />
  return <Outlet />
}

function RequireRole({ role, children }: { role: string; children: React.ReactNode }) {
  const { user } = useAuth()
  if (user?.role !== role) return <Navigate to="/403" replace />
  return children
}

function DashboardLayout() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const [loggingOut, setLoggingOut] = useState(false)
  const isAdmin = user?.role === 'ADMIN'
  const adminPageTitles: Record<string, string> = {
    '/admin/dashboard': 'Tổng quan',
    '/admin/khu-tro': 'Khu trọ',
    '/admin/loai-phong': 'Loại phòng',
    '/admin/phong-tro': 'Phòng trọ',
    '/admin/nguoi-thue': 'Người thuê',
    '/admin/dat-phong': 'Yêu cầu đặt phòng',
    '/tenant/my-room': 'Phòng trọ đang thuê',
    '/tenant/dat-phong': 'Phòng trống và đặt phòng',
  }

  async function signOut() {
    setLoggingOut(true)
    try { await logout() } finally { setLoggingOut(false) }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link className="brand" to={isAdmin ? '/admin/dashboard' : '/tenant/dashboard'}>
          <span className="brand-mark"><Building2 size={21} strokeWidth={2.2} /></span>
          <span className="brand-copy"><strong>phòng trọ</strong><small>QUẢN LÝ VẬN HÀNH</small></span>
        </Link>
        <div className="nav-caption">{isAdmin ? 'QUẢN TRỊ HỆ THỐNG' : 'KHÔNG GIAN CỦA BẠN'}</div>
        <nav className="side-nav" aria-label="Điều hướng chính">
          <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to={isAdmin ? '/admin/dashboard' : '/tenant/dashboard'}>
            <LayoutDashboard size={17} /> Tổng quan
          </NavLink>
          {isAdmin && <>
            <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/admin/khu-tro">
              <Building2 size={17} /> Khu trọ
            </NavLink>
            <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/admin/loai-phong">
              <Shapes size={17} /> Loại phòng
            </NavLink>
            <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/admin/phong-tro">
              <House size={17} /> Phòng trọ
            </NavLink>
            <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/admin/nguoi-thue">
              <Users size={17} /> Người thuê
            </NavLink>
            <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/admin/dat-phong">
              <CalendarDays size={17} /> Đặt phòng
            </NavLink>
          </>}
          {!isAdmin && <>
            <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/tenant/my-room">
              <House size={17} /> Phòng trọ đang thuê
            </NavLink>
            <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/tenant/dat-phong">
              <CalendarDays size={17} /> Phòng trống
            </NavLink>
          </>}
          <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/account/password">
            <KeyRound size={17} /> Đổi mật khẩu
          </NavLink>
          <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/health">
            <Activity size={17} /> Kết nối hệ thống
          </NavLink>
        </nav>
        <div className="sidebar-bottom"><div className="sidebar-rule" /><span className="sidebar-label">VAI TRÒ</span><code>{user?.role}</code><div className="sidebar-version"><span className="version-dot" /> Phiên đăng nhập bảo mật</div></div>
      </aside>
      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb"><span>Không gian làm việc</span><span className="crumb-separator">/</span><strong>{isAdmin ? adminPageTitles[location.pathname] ?? 'Quản trị' : 'Người thuê'}</strong></div>
          <div className="topbar-right">
            <span className="account-name">{user?.fullName}</span>
            <span className="topbar-divider" />
            <button className="logout-button" type="button" onClick={() => void signOut()} disabled={loggingOut} title="Đăng xuất"><LogOut size={16} /><span>{loggingOut ? 'Đang đăng xuất' : 'Đăng xuất'}</span></button>
          </div>
        </header>
        <Outlet />
      </main>
    </div>
  )
}

function RouteLoading() {
  return <div className="route-loading"><span className="button-spinner" /> Đang xác minh phiên đăng nhập</div>
}
