import { useState, type FormEvent } from 'react'
import { KeyRound } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from './api'
import { useAuth } from './useAuth'

export default function PasswordChangePage() {
  const { changePassword } = useAuth()
  const navigate = useNavigate()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')

    if (new TextEncoder().encode(newPassword).length < 12 || new TextEncoder().encode(newPassword).length > 72) {
      setError('Mật khẩu mới cần từ 12 đến 72 byte UTF-8.')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.')
      return
    }

    setSubmitting(true)
    try {
      await changePassword(currentPassword, newPassword, confirmPassword)
      navigate('/login', { replace: true, state: { message: 'Mật khẩu đã được đổi. Vui lòng đăng nhập lại.' } })
    } catch (cause: unknown) {
      setError(cause instanceof ApiError ? cause.message : 'Không đổi được mật khẩu. Vui lòng thử lại.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="work-page">
      <section className="work-heading">
        <div><span className="work-eyebrow">TÀI KHOẢN / BẢO MẬT</span><h1>Đổi mật khẩu</h1><p>Sau khi đổi mật khẩu, mọi phiên đăng nhập hiện tại sẽ bị thu hồi.</p></div>
      </section>
      <section className="work-section password-change-panel">
        <div className="work-section-heading"><div><span><KeyRound size={14} /></span><h2>Cập nhật thông tin bảo mật</h2></div><small>YÊU CẦU ĐĂNG NHẬP LẠI</small></div>
        <form className="password-change-form" onSubmit={(event) => void submit(event)} noValidate>
          <label><span>Mật khẩu hiện tại</span><input type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required maxLength={72} /></label>
          <label><span>Mật khẩu mới</span><input type="password" autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} required minLength={12} maxLength={72} /></label>
          <label><span>Xác nhận mật khẩu mới</span><input type="password" autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} required minLength={12} maxLength={72} /></label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="property-primary" type="submit" disabled={submitting || !currentPassword || !newPassword || !confirmPassword}>{submitting ? 'Đang cập nhật…' : 'Đổi mật khẩu'}</button>
        </form>
      </section>
    </div>
  )
}
