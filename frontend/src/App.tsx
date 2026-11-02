import { useEffect, useState } from 'react'
import { Activity, ArrowUpRight, Building2, Check, CircleAlert, Database, RefreshCw, Server, ShieldCheck, WifiOff } from 'lucide-react'
import './dashboard.css'

type HealthResponse = {
  status: 'UP' | 'DOWN'
  database: string | null
  errorCode: string | null
  message: string | null
}

const errorLabels: Record<string, string> = {
  AUTHENTICATION_FAILED: 'Thông tin xác thực không được SQL Server chấp nhận.',
  DATABASE_NOT_FOUND: 'Không tìm thấy database được cấu hình.',
  DRIVER_OR_INTEGRATED_AUTH_ERROR: 'Thiếu hoặc không nạp được thành phần xác thực JDBC trên Windows.',
  INSTANCE_OR_SERVER_NOT_FOUND: 'Không tìm thấy SQL Server hoặc instance được cấu hình.',
  NETWORK_ERROR: 'Không kết nối được tới listener TCP của SQL Server.',
  QUERY_FAILED: 'Kết nối được nhưng truy vấn kiểm tra database thất bại.',
}

function App() {
  const [requestId, setRequestId] = useState(0)
  const [loading, setLoading] = useState(true)
  const [health, setHealth] = useState<HealthResponse | null>(null)
  const [networkError, setNetworkError] = useState(false)
  const [checkedAt, setCheckedAt] = useState<Date | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    const timeoutId = window.setTimeout(() => controller.abort(), 8000)
    let active = true

    async function checkDatabase() {
      try {
        const response = await fetch('/api/v1/health/database', {
          signal: controller.signal,
          headers: { Accept: 'application/json' },
        })
        const data = await response.json() as HealthResponse

        if (!response.ok && response.status !== 503) {
          throw new Error(`HTTP ${response.status}`)
        }

        if (active) {
          setHealth(data)
          setNetworkError(false)
        }
      } catch {
        if (active) {
          setHealth(null)
          setNetworkError(true)
        }
      } finally {
        window.clearTimeout(timeoutId)
        if (active) {
          setCheckedAt(new Date())
          setLoading(false)
        }
      }
    }

    void checkDatabase()

    return () => {
      active = false
      controller.abort()
    }
  }, [requestId])

  const isUp = health?.status === 'UP'
  const isDown = health?.status === 'DOWN'
  const lastChecked = checkedAt
    ? new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(checkedAt)
    : 'Chưa có dữ liệu'
  const diagnosis = networkError
    ? 'Không nhận được phản hồi từ backend. Kiểm tra dịch vụ API và thử lại.'
    : health?.errorCode
      ? errorLabels[health.errorCode] ?? health.message ?? 'Backend báo lỗi khi kiểm tra database.'
      : 'API đã thực hiện truy vấn SELECT DB_NAME() thành công.'

  function refreshHealth() {
    setLoading(true)
    setRequestId((current) => current + 1)
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#overview" aria-label="Quản lý phòng trọ, trang tổng quan">
          <span className="brand-mark"><Building2 size={21} strokeWidth={2.2} /></span>
          <span className="brand-copy">
            <strong>phòng trọ</strong>
            <small>QUẢN LÝ VẬN HÀNH</small>
          </span>
        </a>

        <div className="nav-caption">KHÔNG GIAN LÀM VIỆC</div>
        <nav className="side-nav" aria-label="Điều hướng chính">
          <a className="nav-link" href="#overview"><Activity size={17} /> Tổng quan</a>
          <a className="nav-link active" href="#connection" aria-current="page"><Database size={17} /> Kết nối dữ liệu</a>
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-rule" />
          <span className="sidebar-label">API</span>
          <code>/api/v1</code>
          <div className="sidebar-version"><span className="version-dot" /> Môi trường phát triển</div>
        </div>
      </aside>

      <main className="main-area" id="overview">
        <header className="topbar">
          <div className="breadcrumb"><span>Hệ thống</span><span className="crumb-separator">/</span><strong>Kết nối dữ liệu</strong></div>
          <div className="topbar-right">
            <span className="secure-label"><span className="secure-dot" /> KẾT NỐI NỘI BỘ</span>
            <span className="topbar-divider" />
            <span className="topbar-date">19 THÁNG 10, 2026</span>
          </div>
        </header>

        <div className="page-content" id="connection">
          <section className="page-heading">
            <div>
              <div className="eyebrow"><span>HỆ THỐNG</span><span className="eyebrow-line" /><span>01 / HEALTH</span></div>
              <h1>Kết nối cơ sở dữ liệu</h1>
              <p className="page-subtitle">Trạng thái truy vấn backend tới SQL Server.</p>
            </div>
            <button className="refresh-button" type="button" onClick={refreshHealth} disabled={loading} title="Kiểm tra lại kết nối">
              <RefreshCw size={16} className={loading ? 'spin' : ''} />
              <span>{loading ? 'Đang kiểm tra' : 'Kiểm tra lại'}</span>
            </button>
          </section>

          <section className={`status-panel ${isUp ? 'is-up' : isDown ? 'is-down' : ''}`} aria-live="polite">
            <div className="status-panel-copy">
              <div className="status-kicker"><span className="status-pulse" /> TÌNH TRẠNG HIỆN TẠI</div>
              <h2>{loading ? 'Đang kiểm tra kết nối' : isUp ? 'Database đang hoạt động' : isDown ? 'Database chưa sẵn sàng' : 'Chưa kết nối được API'}</h2>
              <p>{loading ? 'Đang gửi yêu cầu kiểm tra tới backend.' : diagnosis}</p>
              <div className="status-meta">
                <span className="status-pill">
                  {loading ? <RefreshCw size={13} className="spin" /> : isUp ? <Check size={13} /> : <CircleAlert size={13} />}
                  {loading ? 'ĐANG KIỂM TRA' : isUp ? 'ĐÃ KẾT NỐI' : isDown ? 'DATABASE DOWN' : 'API KHÔNG PHẢN HỒI'}
                </span>
                {health?.errorCode && <code className="error-code">{health.errorCode}</code>}
              </div>
            </div>
            <div className="status-art" aria-hidden="true">
              <div className="art-orbit orbit-one" />
              <div className="art-orbit orbit-two" />
              <div className="art-core">{isUp ? <Database size={31} /> : networkError ? <WifiOff size={31} /> : <Server size={31} />}</div>
              <span className="art-node node-top" />
              <span className="art-node node-bottom" />
              <span className="art-cross cross-one">+</span>
              <span className="art-cross cross-two">+</span>
            </div>
          </section>

          <section className="details-section" aria-labelledby="details-heading">
            <div className="section-heading">
              <div>
                <span className="section-index">01</span>
                <h2 id="details-heading">Chi tiết kiểm tra</h2>
              </div>
              <span className="section-note">LIVE PROBE</span>
            </div>

            <div className="details-table">
              <div className="detail-row">
                <span className="detail-label"><Activity size={15} /> Endpoint</span>
                <code className="detail-value">GET /api/v1/health/database</code>
                <a className="open-endpoint" href="/api/v1/health/database" target="_blank" rel="noreferrer" aria-label="Mở response health API">
                  Xem response <ArrowUpRight size={14} />
                </a>
              </div>
              <div className="detail-row">
                <span className="detail-label"><Database size={15} /> Database</span>
                <span className="detail-value">{health?.database ?? 'Chưa xác nhận'}</span>
                <span className={`detail-state ${isUp ? 'good' : ''}`}><span />{isUp ? 'ONLINE' : loading ? 'ĐANG KIỂM TRA' : 'CHƯA XÁC NHẬN'}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label"><Server size={15} /> HTTP status</span>
                <span className="detail-value">{loading ? '—' : networkError ? 'Không phản hồi' : isUp ? '200 OK' : '503 Service Unavailable'}</span>
                <span className="detail-mono">{health?.errorCode ?? (networkError ? 'API_UNREACHABLE' : '—')}</span>
              </div>
              <div className="detail-row last-row">
                <span className="detail-label"><RefreshCw size={15} /> Lần kiểm tra gần nhất</span>
                <span className="detail-value">{lastChecked}</span>
                <span className="detail-mono">GIỜ ĐỊA PHƯƠNG</span>
              </div>
            </div>
          </section>

          <footer className="page-footer">
            <span><ShieldCheck size={15} /> Không hiển thị thông tin xác thực trong response.</span>
            <span className="footer-mark">QLPT <span>·</span> SYSTEM STATUS</span>
          </footer>
        </div>
      </main>
    </div>
  )
}

export default App
