import { useState, type FormEvent } from 'react'
import { Eye, EyeOff, KeyRound, UserPlus } from 'lucide-react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from './api'
import type { RegisterValues } from './auth-context'
import { useAuth } from './useAuth'
import './auth.css'

function routeForRole(role: string) {
  if (role === 'ADMIN') return '/admin/dashboard'
  if (role === 'NGUOI_THUE') return '/tenant/dashboard'
  return '/403'
}

export function LoginPage() {
  const { login, status, user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const stateMessage = typeof location.state === 'object' && location.state !== null
    && 'message' in location.state && typeof location.state.message === 'string'
    ? location.state.message
    : null
  const [loginValue, setLoginValue] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  if (status === 'checking') return <AuthLoading />
  if (user) return <Navigate to={routeForRole(user.role)} replace />

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (!loginValue.trim() || !password) {
      setError('Vui lòng nhập tên đăng nhập/email và mật khẩu.')
      return
    }

    setSubmitting(true)
    try {
      const currentUser = await login(loginValue, password)
      navigate(routeForRole(currentUser.role), { replace: true })
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Đăng nhập chưa thành công. Vui lòng thử lại.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthFrame eyebrow="CỔNG QUẢN LÝ" title="Đăng nhập" description="Đăng nhập để tiếp tục tới không gian làm việc của bạn.">
      <form className="auth-form" onSubmit={submit} noValidate>
        <label className="auth-field">
          <span>Tên đăng nhập hoặc email</span>
          <input autoComplete="username" value={loginValue} onChange={(event) => setLoginValue(event.target.value)} placeholder="Nhập tên đăng nhập hoặc email" />
        </label>
        <label className="auth-field">
          <span>Mật khẩu</span>
          <span className="password-input">
            <input type={showPassword ? 'text' : 'password'} autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Nhập mật khẩu" />
            <button className="password-toggle" type="button" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>
              {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
            </button>
          </span>
        </label>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="auth-submit" type="submit" disabled={submitting}>
          {submitting ? <span className="button-spinner" /> : <KeyRound size={17} />}
          {submitting ? 'Đang đăng nhập' : 'Đăng nhập'}
        </button>
      </form>
      {stateMessage && <p className="auth-success" role="status">{stateMessage}</p>}
      <p className="auth-switch">Chưa có tài khoản? <Link to="/register">Đăng ký người thuê</Link></p>
      <p className="auth-switch">Muốn xem phòng trước? <Link to="/rooms">Xem phòng trống</Link></p>
    </AuthFrame>
  )
}

export function RegisterPage() {
  const { register, status, user } = useAuth()
  const navigate = useNavigate()
  const [values, setValues] = useState<RegisterValues & { citizenId: string; birthDate: string; address: string }>({
    username: '', email: '', phone: '', password: '', confirmPassword: '', fullName: '', citizenId: '', birthDate: '', address: '',
  })
  const [showPassword, setShowPassword] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  if (status === 'checking') return <AuthLoading />
  if (user) return <Navigate to={routeForRole(user.role)} replace />

  function update(field: keyof typeof values, value: string) {
    setValues((current) => ({ ...current, [field]: value }))
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (!values.fullName.trim() || !values.username.trim() || !values.password) {
      setError('Vui lòng nhập họ tên, tên đăng nhập và mật khẩu.')
      return
    }
    if (values.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email)) {
      setError('Email chưa đúng định dạng.')
      return
    }
    if (values.phone && !/^0[0-9]{9,10}$/.test(values.phone)) {
      setError('Số điện thoại cần bắt đầu bằng 0 và có 10-11 chữ số.')
      return
    }
    if (values.citizenId && !/^[0-9]{9,12}$/.test(values.citizenId)) {
      setError('Số CCCD chỉ gồm 9-12 chữ số.')
      return
    }
    if (values.password.length < 12 || new TextEncoder().encode(values.password).length > 72) {
      setError('Mật khẩu cần từ 12 ký tự và không vượt quá giới hạn được hỗ trợ.')
      return
    }
    if (values.password !== values.confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.')
      return
    }

    setSubmitting(true)
    try {
      const { citizenId, birthDate, address, ...account } = values
      await register({
        ...account,
        citizenId: citizenId || undefined,
        birthDate: birthDate || undefined,
        address: address || undefined,
      } as RegisterValues)
      navigate('/tenant/dashboard', { replace: true })
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Đăng ký chưa thành công. Vui lòng thử lại.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthFrame eyebrow="TẠO TÀI KHOẢN" title="Đăng ký người thuê" description="Tài khoản mới được cấp quyền người thuê, không thể tự chọn quyền quản trị.">
      <form className="auth-form register-form" onSubmit={submit} noValidate>
        <div className="form-grid">
          <label className="auth-field span-two"><span>Họ và tên *</span><input autoComplete="name" maxLength={150} value={values.fullName} onChange={(event) => update('fullName', event.target.value)} /></label>
          <label className="auth-field"><span>Tên đăng nhập *</span><input autoComplete="username" minLength={3} maxLength={100} value={values.username} onChange={(event) => update('username', event.target.value)} /></label>
          <label className="auth-field"><span>Số điện thoại</span><input autoComplete="tel" maxLength={20} value={values.phone} onChange={(event) => update('phone', event.target.value)} /></label>
          <label className="auth-field span-two"><span>Email</span><input type="email" autoComplete="email" maxLength={254} value={values.email} onChange={(event) => update('email', event.target.value)} /></label>
          <label className="auth-field"><span>Mật khẩu *</span><span className="password-input"><input type={showPassword ? 'text' : 'password'} autoComplete="new-password" maxLength={72} value={values.password} onChange={(event) => update('password', event.target.value)} /><button className="password-toggle" type="button" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>{showPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></span></label>
          <label className="auth-field"><span>Xác nhận mật khẩu *</span><input type="password" autoComplete="new-password" maxLength={72} value={values.confirmPassword} onChange={(event) => update('confirmPassword', event.target.value)} /></label>
          <label className="auth-field"><span>Số CCCD</span><input maxLength={20} value={values.citizenId} onChange={(event) => update('citizenId', event.target.value)} /></label>
          <label className="auth-field"><span>Ngày sinh</span><input type="date" value={values.birthDate} onChange={(event) => update('birthDate', event.target.value)} /></label>
          <label className="auth-field span-two"><span>Địa chỉ</span><input maxLength={500} value={values.address} onChange={(event) => update('address', event.target.value)} /></label>
        </div>
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="auth-submit" type="submit" disabled={submitting}>
          {submitting ? <span className="button-spinner" /> : <UserPlus size={17} />}
          {submitting ? 'Đang tạo tài khoản' : 'Tạo tài khoản'}
        </button>
      </form>
      <p className="auth-switch">Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
    </AuthFrame>
  )
}

export function ForbiddenPage() {
  const { logout } = useAuth()
  return (
    <AuthFrame eyebrow="403 / KHÔNG ĐƯỢC PHÉP" title="Tài khoản chưa có quyền truy cập" description="Vai trò hiện tại chưa được cấp dashboard trong phiên bản này.">
      <button className="auth-submit" type="button" onClick={() => void logout()}>Đăng xuất</button>
    </AuthFrame>
  )
}

function AuthFrame({ eyebrow, title, description, children }: { eyebrow: string; title: string; description: string; children: React.ReactNode }) {
  return (
    <main className="auth-page">
      <div className="auth-visual" aria-hidden="true">
        <div className="visual-grid" />
        <div className="visual-building"><span /><span /><span /><span /><span /><span /><span /><span /><span /></div>
        <div className="visual-caption"><span>NHÀ Ở</span><i /> <span>VẬN HÀNH</span></div>
      </div>
      <section className="auth-panel">
        <Link className="auth-brand" to="/login"><span className="auth-brand-mark">P</span><span><b>phòng trọ</b><small>QUẢN LÝ VẬN HÀNH</small></span></Link>
        <div className="auth-content">
          <div className="auth-eyebrow">{eyebrow}</div>
          <h1>{title}</h1>
          <p className="auth-description">{description}</p>
          {children}
        </div>
        <footer className="auth-footnote">QUẢN LÝ PHÒNG TRỌ <span>·</span> CỔNG TÀI KHOẢN</footer>
      </section>
    </main>
  )
}

function AuthLoading() {
  return <main className="auth-loading"><span className="button-spinner" /> Đang xác minh phiên đăng nhập</main>
}
