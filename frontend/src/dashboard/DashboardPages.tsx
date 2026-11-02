import { useEffect, useState } from 'react'
import { Activity, Building2, CircleAlert, FileText, House, RefreshCw, ShieldCheck, Users } from 'lucide-react'
import { ApiError, apiRequest, type AdminDashboard, type TenantDashboard, type TenantPlan } from '../auth/api'
import { useAuth } from '../auth/useAuth'

export function AdminDashboardPage() {
  const { token, user } = useAuth()
  const [data, setData] = useState<AdminDashboard | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    void apiRequest<AdminDashboard>('/admin/dashboard', { signal: controller.signal }, token ?? undefined)
      .then((result) => { if (active) setData(result) })
      .catch((cause: unknown) => { if (active) setError(cause instanceof ApiError ? cause.message : 'Không tải được dashboard.') })
      .finally(() => { if (active) setLoading(false) })

    return () => { active = false; controller.abort() }
  }, [retry, token])

  function refreshDashboard() {
    setLoading(true)
    setError('')
    setRetry((value) => value + 1)
  }

  return (
    <div className="work-page">
      <section className="work-heading">
        <div><span className="work-eyebrow">QUẢN TRỊ / TỔNG QUAN</span><h1>Xin chào, {user?.fullName}</h1><p>Số liệu khu trọ, phòng và tài khoản lấy từ SQL Server.</p></div>
        <button className="refresh-button" type="button" onClick={refreshDashboard} disabled={loading} title="Tải lại số liệu"><RefreshCw size={16} className={loading ? 'spin' : ''} /><span>Tải lại</span></button>
      </section>
      {error && <div className="dashboard-error" role="alert"><CircleAlert size={18} /><span>{error}</span></div>}
      <div className="metric-grid">
        <Metric title="Khu trọ" value={data?.totalSites} icon={<Building2 size={18} />} accent="blue" loading={loading} />
        <Metric title="Tổng số phòng" value={data?.totalRooms} icon={<House size={18} />} accent="green" loading={loading} />
        <Metric title="Phòng trống" value={data?.vacantRooms} icon={<House size={18} />} accent="amber" loading={loading} />
        <Metric title="Đang cho thuê" value={data?.occupiedRooms} icon={<Users size={18} />} accent="cyan" loading={loading} />
      </div>
      <section className="work-section">
        <div className="work-section-heading"><div><span>01</span><h2>Tài khoản và hệ thống</h2></div><small>API / DATABASE</small></div>
        <div className="work-info-row"><div><Users size={17} /><span>Tổng tài khoản</span></div><strong>{loading ? 'Đang tải' : data?.totalAccounts ?? '—'}</strong></div>
        <div className="work-info-row"><div><Activity size={17} /><span>Tài khoản đang hoạt động / bị khóa</span></div><strong>{loading ? 'Đang tải' : `${data?.activeAccounts ?? '—'} / ${data?.lockedAccounts ?? '—'}`}</strong></div>
        <div className="work-info-row"><div><ShieldCheck size={17} /><span>Admin / người thuê</span></div><strong>{loading ? 'Đang tải' : `${data?.adminAccounts ?? '—'} / ${data?.tenantAccounts ?? '—'}`}</strong></div>
        <div className="work-info-row"><div><Activity size={17} /><span>Kiểm tra kết nối SQL Server</span></div><a href="/health">Mở kiểm tra <span aria-hidden="true">↗</span></a></div>
        <div className="work-info-row"><div><FileText size={17} /><span>Khu trọ, loại phòng và phòng trọ</span></div><a href="/admin/khu-tro">Mở quản lý <span aria-hidden="true">↗</span></a></div>
        <div className="work-info-row last"><div><Users size={17} /><span>Hồ sơ người thuê</span></div><a href="/admin/nguoi-thue">Mở danh sách <span aria-hidden="true">↗</span></a></div>
      </section>
      <section className="work-section">
        <div className="work-section-heading"><div><span>02</span><h2>Nghiệp vụ chưa khả dụng</h2></div><small>CHƯA CÓ BẢNG/API</small></div>
        <p className="tenant-notice-copy">Hợp đồng, hóa đơn, thanh toán, tổng thu/chi và hoạt động gần đây chưa được cung cấp vì các bảng nghiệp vụ tương ứng chưa có trong schema đã xác minh. Dashboard không hiển thị số liệu minh họa.</p>
      </section>
    </div>
  )
}

export function TenantDashboardPage() {
  const { token, user } = useAuth()
  const [data, setData] = useState<TenantDashboard | null>(null)
  const [plan, setPlan] = useState<TenantPlan | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [planError, setPlanError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    void apiRequest<TenantDashboard>('/tenant/dashboard', { signal: controller.signal }, token ?? undefined)
      .then((result) => { if (active) setData(result) })
      .catch((cause: unknown) => { if (active) setError(cause instanceof ApiError ? cause.message : 'Không tải được hồ sơ người thuê.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false; controller.abort() }
  }, [token])

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    void apiRequest<TenantPlan>('/tenant/plan', { signal: controller.signal }, token ?? undefined)
      .then((result) => { if (active) setPlan(result) })
      .catch((cause: unknown) => {
        if (active) setPlanError(cause instanceof ApiError ? cause.message : 'Không tải được gói sử dụng.')
      })
    return () => { active = false; controller.abort() }
  }, [token])

  return (
    <div className="work-page">
      <section className="work-heading"><div><span className="work-eyebrow">NGƯỜI THUÊ / TỔNG QUAN</span><h1>Xin chào, {user?.fullName}</h1><p>Thông tin hồ sơ gắn với tài khoản đang đăng nhập.</p></div></section>
      {error && <div className="dashboard-error" role="alert"><CircleAlert size={18} /><span>{error}</span></div>}
      {planError && <div className="dashboard-error" role="alert"><CircleAlert size={18} /><span>{planError}</span></div>}
      {loading ? <div className="dashboard-empty">Đang tải hồ sơ…</div> : data ? (
        <>
          <section className="tenant-profile">
            <div className="profile-initial">{data.fullName.trim().charAt(0).toLocaleUpperCase('vi-VN')}</div>
            <div><span className="work-eyebrow">HỒ SƠ NGƯỜI THUÊ</span><h2>{data.fullName}</h2><p>{data.email || 'Chưa cung cấp email'} <span>·</span> {data.phone || 'Chưa cung cấp số điện thoại'}</p></div>
            <span className="profile-role">NGƯỜI THUÊ</span>
          </section>
          <section className="work-section">
            <div className="work-section-heading"><div><span>01</span><h2>Thông tin tài khoản</h2></div><small>HỒ SƠ CỦA TÔI</small></div>
            <div className="profile-grid">
              <ProfileField label="Tên đăng nhập" value={data.username} />
              <ProfileField label="Số CCCD" value={data.citizenId} />
              <ProfileField label="Ngày sinh" value={data.birthDate ? new Intl.DateTimeFormat('vi-VN').format(new Date(`${data.birthDate}T00:00:00`)) : null} />
              <ProfileField label="Địa chỉ" value={data.address} />
            </div>
          </section>
        </>
      ) : <div className="dashboard-empty">Không tìm thấy hồ sơ người thuê gắn với tài khoản này.</div>}
      <section className="work-section">
        <div className="work-section-heading"><div><span>02</span><h2>Gói sử dụng</h2></div><small>QUYỀN LỢI TÀI KHOẢN</small></div>
        {plan ? (
          <>
            <div className="work-info-row"><div><ShieldCheck size={17} /><span>Gói hiện tại</span></div><strong>{plan.name}</strong></div>
            {plan.expiresOn && <div className="work-info-row"><div><Activity size={17} /><span>Hiệu lực đến hết ngày</span></div><strong>{new Intl.DateTimeFormat('vi-VN').format(new Date(`${plan.expiresOn}T00:00:00`))}</strong></div>}
            <p className="tenant-notice-copy">{plan.premium
              ? 'Bao gồm quyền lợi gói Thường và được ưu tiên hiển thị yêu cầu đặt phòng trong hàng đợi Admin.'
              : 'Xem phòng, gửi yêu cầu đặt phòng và theo dõi các yêu cầu của chính bạn.'}</p>
            {plan.premium && <p className="tenant-notice-copy">Yêu cầu đặt phòng được ưu tiên hiển thị trong hàng đợi xử lý của Admin; việc xác nhận vẫn phụ thuộc vào tình trạng phòng và quyết định của Admin.</p>}
          </>
        ) : <div className="dashboard-empty">Đang tải gói sử dụng…</div>}
      </section>
      <section className="work-section tenant-notice">
        <div className="work-section-heading"><div><span>03</span><h2>Phòng trọ và hợp đồng</h2></div><small>HỢP ĐỒNG ĐANG HIỆU LỰC</small></div>
        <p>Phòng trọ được xác định từ hợp đồng đang hiệu lực gắn với tài khoản của bạn. <a href="/tenant/my-room">Xem phòng trọ đang thuê</a>.</p>
      </section>
    </div>
  )
}

function Metric({ title, value, icon, accent, loading }: { title: string; value?: number; icon: React.ReactNode; accent: string; loading: boolean }) {
  return <article className={`metric-card ${accent}`}><span className="metric-icon">{icon}</span><span className="metric-title">{title}</span><strong>{loading ? '—' : value ?? '—'}</strong><span className="metric-caption">{loading ? 'Đang tải dữ liệu' : 'Theo dữ liệu hiện có'}</span></article>
}

function ProfileField({ label, value }: { label: string; value: string | null }) {
  return <div className="profile-field"><span>{label}</span><strong>{value || 'Chưa cung cấp'}</strong></div>
}
