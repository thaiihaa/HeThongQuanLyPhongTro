import { useEffect, useState, type FormEvent } from 'react'
import { CircleAlert, RefreshCw, Search, Users, X } from 'lucide-react'
import { ApiError, apiRequest } from '../auth/api'
import { useAuth } from '../auth/useAuth'
import './admin-tenant.css'

type TenantSummary = {
  id: number
  userId: number
  username: string
  fullName: string
  email: string | null
  phone: string | null
  accountStatus: string
  birthDate: string | null
  address: string | null
  planCode: 'THUONG' | 'PREMIUM'
  planExpiresOn: string | null
}

type TenantPageResult = {
  content: TenantSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export default function AdminTenantPage() {
  const { token } = useAuth()
  const [data, setData] = useState<TenantPageResult | null>(null)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [reload, setReload] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [planExpiresOn, setPlanExpiresOn] = useState<Record<number, string>>({})
  const [savingPlanUserId, setSavingPlanUserId] = useState<number | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    const query = new URLSearchParams({ page: String(page), size: '20' })
    if (search) query.set('search', search)

    void apiRequest<TenantPageResult>(`/admin/nguoi-thue?${query.toString()}`, {
      signal: controller.signal,
    }, token ?? undefined)
      .then((result) => { if (active) setData(result) })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof ApiError ? cause.message : 'Không tải được danh sách người thuê.')
      })
      .finally(() => { if (active) setLoading(false) })

    return () => { active = false; controller.abort() }
  }, [page, reload, search, token])

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setPage(0)
    const nextSearch = searchInput.trim()
    if (nextSearch === search && page === 0) setReload((current) => current + 1)
    setSearch(nextSearch)
  }

  function clearSearch() {
    setSearchInput('')
    setSearch('')
    setPage(0)
    setLoading(true)
    setError('')
  }

  function retry() {
    setLoading(true)
    setError('')
    setNotice('')
    setReload((current) => current + 1)
  }

  async function savePlan(tenant: TenantSummary) {
    if (!token) return
    const expiresOn = planExpiresOn[tenant.userId]
    if (tenant.planCode !== 'PREMIUM' && !expiresOn) {
      setError('Hãy chọn ngày hết hạn trước khi cấp Premium.')
      return
    }

    setSavingPlanUserId(tenant.userId)
    setError('')
    setNotice('')
    try {
      if (tenant.planCode === 'PREMIUM') {
        await apiRequest<null>(`/admin/nguoi-thue/${tenant.userId}/plan`, { method: 'DELETE' }, token)
        setNotice(`Đã thu hồi gói Premium của ${tenant.username}.`)
      } else {
        await apiRequest(`/admin/nguoi-thue/${tenant.userId}/plan`, {
          method: 'POST',
          body: JSON.stringify({ expiresOn }),
        }, token)
        setNotice(`Đã cấp gói Premium cho ${tenant.username}.`)
        setPlanExpiresOn((current) => ({ ...current, [tenant.userId]: '' }))
      }
      setReload((current) => current + 1)
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Không cập nhật được gói sử dụng.')
    } finally {
      setSavingPlanUserId(null)
    }
  }

  return (
    <div className="work-page">
      <section className="work-heading">
        <div>
          <span className="work-eyebrow">QUẢN TRỊ / NGƯỜI THUÊ</span>
          <h1>Danh sách người thuê</h1>
          <p>Thông tin tài khoản và hồ sơ liên kết từ cơ sở dữ liệu.</p>
        </div>
        <button className="refresh-button" type="button" onClick={retry} disabled={loading} title="Tải lại danh sách">
          <RefreshCw size={16} className={loading ? 'spin' : ''} /><span>Tải lại</span>
        </button>
      </section>

      <form className="tenant-search" onSubmit={submitSearch}>
        <label htmlFor="tenant-search-input">Tìm theo họ tên, tài khoản, email hoặc số điện thoại</label>
        <div className="tenant-search-controls">
          <div className="tenant-search-input">
            <Search size={17} aria-hidden="true" />
            <input
              id="tenant-search-input"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              maxLength={150}
              placeholder="Nhập từ khóa tìm kiếm"
            />
            {searchInput && <button type="button" onClick={clearSearch} aria-label="Xóa từ khóa"><X size={16} /></button>}
          </div>
          <button className="property-primary" type="submit" disabled={loading}><Search size={16} /> Tìm kiếm</button>
        </div>
      </form>

      {error && (
        <div className="dashboard-error" role="alert">
          <CircleAlert size={18} /><span>{error}</span>
          <button type="button" onClick={retry}>Thử lại</button>
        </div>
      )}
      {notice && <div className="tenant-success" role="status">{notice}</div>}

      <section className="tenant-table-panel" aria-busy={loading}>
        <div className="tenant-table-heading">
          <div><Users size={17} /><strong>Hồ sơ người thuê</strong></div>
          <span>{loading ? 'Đang tải…' : `${data?.totalElements ?? 0} kết quả`}</span>
        </div>
        {loading ? <div className="tenant-state">Đang tải danh sách…</div> : data && data.content.length > 0 ? (
          <>
            <div className="tenant-table-scroll">
              <table className="tenant-table">
                <thead><tr><th>Người thuê</th><th>Liên hệ</th><th>Ngày sinh</th><th>Địa chỉ</th><th>Gói</th><th>Trạng thái</th></tr></thead>
                <tbody>
                  {data.content.map((tenant) => (
                    <tr key={tenant.id}>
                      <td><strong>{tenant.fullName}</strong><span className="tenant-username">{tenant.username}</span></td>
                      <td><span>{tenant.email || 'Chưa có email'}</span><span>{tenant.phone || 'Chưa có số điện thoại'}</span></td>
                      <td>{tenant.birthDate ? new Intl.DateTimeFormat('vi-VN').format(new Date(`${tenant.birthDate}T00:00:00`)) : '—'}</td>
                      <td>{tenant.address || '—'}</td>
                      <td className="tenant-plan-cell">
                        <span className={`tenant-plan-badge ${tenant.planCode === 'PREMIUM' ? 'premium' : ''}`}>{tenant.planCode === 'PREMIUM' ? 'Premium' : 'Thường'}</span>
                        {tenant.planCode === 'PREMIUM'
                          ? <>
                            <small>Đến hết {tenant.planExpiresOn ? new Intl.DateTimeFormat('vi-VN').format(new Date(`${tenant.planExpiresOn}T00:00:00`)) : '—'}</small>
                            <button type="button" className="tenant-plan-action" disabled={savingPlanUserId === tenant.userId} onClick={() => void savePlan(tenant)}>{savingPlanUserId === tenant.userId ? 'Đang lưu…' : 'Thu hồi Premium'}</button>
                          </>
                          : <>
                            <label className="tenant-plan-expiry">Hết hạn
                              <input
                                type="date"
                                min={new Date().toISOString().slice(0, 10)}
                                value={planExpiresOn[tenant.userId] ?? ''}
                                onChange={(event) => setPlanExpiresOn((current) => ({
                                  ...current,
                                  [tenant.userId]: event.target.value,
                                }))}
                              />
                            </label>
                            <button type="button" className="tenant-plan-action" disabled={savingPlanUserId === tenant.userId} onClick={() => void savePlan(tenant)}>{savingPlanUserId === tenant.userId ? 'Đang lưu…' : 'Cấp Premium'}</button>
                          </>}
                      </td>
                      <td><span className={`tenant-status ${tenant.accountStatus === 'HOAT_DONG' ? 'active' : tenant.accountStatus === 'KHOA' ? 'locked' : 'unknown'}`}>{tenant.accountStatus === 'HOAT_DONG' ? 'Hoạt động' : tenant.accountStatus === 'KHOA' ? 'Đã khóa' : tenant.accountStatus || 'Chưa xác định'}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="tenant-pagination">
              <span>Trang {data.page + 1} / {Math.max(data.totalPages, 1)}</span>
              <div>
                <button type="button" disabled={data.page <= 0} onClick={() => { setLoading(true); setPage((current) => current - 1) }}>Trước</button>
                <button type="button" disabled={data.totalPages === 0 || data.page + 1 >= data.totalPages} onClick={() => { setLoading(true); setPage((current) => current + 1) }}>Tiếp</button>
              </div>
            </div>
          </>
        ) : (
          <div className="tenant-state">
            <Users size={24} />
            <strong>{search ? 'Không tìm thấy hồ sơ phù hợp' : 'Chưa có hồ sơ người thuê'}</strong>
            <span>{search ? 'Thử từ khóa khác hoặc xóa bộ lọc.' : 'Tài khoản đăng ký mới sẽ xuất hiện sau khi tạo hồ sơ người thuê.'}</span>
          </div>
        )}
      </section>
      <p className="tenant-privacy-note">Danh sách không hiển thị CCCD, mật khẩu hoặc thông tin xác thực.</p>
    </div>
  )
}
