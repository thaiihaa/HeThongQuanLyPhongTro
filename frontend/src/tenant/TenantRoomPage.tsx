import { useEffect, useState } from 'react'
import { ArrowLeft, ArrowRight, CircleAlert, MapPin, MoveUpRight, RefreshCw, Search, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import { ApiError, apiRequest, type TenantRoom } from '../auth/api'
import { useAuth } from '../auth/useAuth'
import './tenant-room.css'

const placeholderImage = '/room-placeholder.svg'

export default function TenantRoomPage() {
  const { token } = useAuth()
  const [room, setRoom] = useState<TenantRoom | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const [selectedImage, setSelectedImage] = useState(0)
  const [lightboxOpen, setLightboxOpen] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    void apiRequest<TenantRoom | null>('/tenant/my-room', { signal: controller.signal }, token ?? undefined)
      .then((result) => {
        if (active) {
          setRoom(result)
          setSelectedImage(0)
        }
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof ApiError ? cause.message : 'Không tải được thông tin phòng đang thuê.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false; controller.abort() }
  }, [retry, token])

  const images = room?.media.filter((item) => item.mediaType === 'IMAGE') ?? []
  const videos = room?.media.filter((item) => item.mediaType === 'VIDEO') ?? []
  const activeImage = images[selectedImage]

  function moveImage(direction: -1 | 1) {
    if (images.length < 1) return
    setSelectedImage((index) => (index + direction + images.length) % images.length)
  }

  function formatMoney(value: number) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value)
  }

  function formatDate(value: string | null) {
    return value ? new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`)) : 'Không xác định thời hạn'
  }

  return (
    <div className="work-page tenant-room-page">
      <section className="work-heading">
        <div>
          <span className="work-eyebrow">NGƯỜI THUÊ / CHỖ Ở CỦA TÔI</span>
          <h1>Phòng trọ đang thuê</h1>
          <p>Thông tin được lấy từ hợp đồng đang hiệu lực của tài khoản hiện tại.</p>
        </div>
        {!loading && room && <Link className="tenant-room-secondary-link" to="/tenant/dat-phong"><Search size={16} /> Tìm phòng khác</Link>}
      </section>

      {error && (
        <div className="dashboard-error" role="alert">
          <CircleAlert size={18} /><span>{error}</span>
          <button type="button" className="tenant-room-retry" onClick={() => {
            setLoading(true)
            setError('')
            setRetry((value) => value + 1)
          }} disabled={loading}>
            <RefreshCw size={15} /> Thử lại
          </button>
        </div>
      )}

      {loading ? (
        <div className="dashboard-empty tenant-room-state" role="status"><span className="button-spinner" /> Đang tải thông tin phòng thuê…</div>
      ) : error ? null : room ? (
        <>
          <section className="tenant-room-overview">
            <div className="tenant-room-hero">
              {activeImage ? (
                <button className="tenant-room-hero-image" type="button" onClick={() => setLightboxOpen(true)} aria-label="Phóng to ảnh phòng">
                  <img src={activeImage.url} alt={activeImage.description || `Phòng ${room.roomCode}`} onError={(event) => { event.currentTarget.src = placeholderImage }} />
                  <span className="tenant-room-photo-hint">Nhấn để phóng to</span>
                </button>
              ) : (
                <img className="tenant-room-hero-placeholder" src={placeholderImage} alt={`Phòng ${room.roomCode}`} />
              )}
              {images.length > 1 && (
                <div className="tenant-room-carousel-controls">
                  <button type="button" onClick={() => moveImage(-1)} aria-label="Ảnh trước"><ArrowLeft size={17} /></button>
                  <span>{selectedImage + 1} / {images.length}</span>
                  <button type="button" onClick={() => moveImage(1)} aria-label="Ảnh tiếp theo"><ArrowRight size={17} /></button>
                </div>
              )}
            </div>
            <div className="tenant-room-summary">
              <span className="tenant-room-status">{room.status === 'DANG_THUE' ? 'ĐANG THUÊ' : room.status}</span>
              <h2>Phòng {room.roomCode}</h2>
              <p className="tenant-room-site"><MapPin size={16} /> {room.siteName}</p>
              <p className="tenant-room-address">{room.address}</p>
              <div className="tenant-room-price">{formatMoney(room.monthlyRent)}<span>/ tháng</span></div>
              <div className="tenant-room-quick-facts">
                <div><span>Diện tích</span><strong>{room.areaSquareMeters} m²</strong></div>
                <div><span>Bắt đầu thuê</span><strong>{formatDate(room.leaseStartsOn)}</strong></div>
                <div><span>Kết thúc hợp đồng</span><strong>{formatDate(room.leaseEndsOn)}</strong></div>
              </div>
            </div>
          </section>

          {images.length > 1 && (
            <div className="tenant-room-thumbnails" aria-label="Thư viện ảnh phòng">
              {images.map((image, index) => (
                <button key={image.id} type="button" className={index === selectedImage ? 'selected' : ''} onClick={() => setSelectedImage(index)} aria-label={`Xem ảnh ${index + 1}`}>
                  <img src={image.url} alt={image.description || image.originalFileName} loading="lazy" onError={(event) => { event.currentTarget.src = placeholderImage }} />
                </button>
              ))}
            </div>
          )}

          {room.description && <section className="tenant-room-section"><h3>Giới thiệu phòng</h3><p className="tenant-room-description">{room.description}</p></section>}

          <div className="tenant-room-detail-grid">
            <section className="tenant-room-section">
              <h3>Tiện nghi</h3>
              {room.amenities.length ? <ul className="tenant-room-list">{room.amenities.map((item, index) => <li key={`${item.name}-${index}`}><span>{item.name}</span><strong>{item.quantity > 1 ? `× ${item.quantity}` : item.condition || 'Có sẵn'}</strong></li>)}</ul> : <p className="tenant-room-muted">Chưa có thông tin tiện nghi.</p>}
            </section>
            <section className="tenant-room-section">
              <h3>Nội thất và tài sản</h3>
              {room.assets.length ? <ul className="tenant-room-list">{room.assets.map((item, index) => <li key={`${item.name}-${index}`}><span>{item.name}</span><strong>{item.quantity > 1 ? `× ${item.quantity}` : item.condition || 'Có sẵn'}</strong></li>)}</ul> : <p className="tenant-room-muted">Chưa có thông tin tài sản.</p>}
            </section>
            <section className="tenant-room-section">
              <h3>Dịch vụ và đơn giá</h3>
              {room.services.length ? <ul className="tenant-room-list">{room.services.map((item, index) => <li key={`${item.name}-${index}`}><span>{item.name}<small>{item.calculationMethod} · {item.unit}</small></span><strong>{formatMoney(item.appliedPrice)}</strong></li>)}</ul> : <p className="tenant-room-muted">Chưa có dịch vụ được liên kết với hợp đồng.</p>}
            </section>
          </div>

          {videos.length > 0 && (
            <section className="tenant-room-section tenant-room-video-section">
              <h3>Video giới thiệu phòng</h3>
              <div className="tenant-room-video-grid">
                {videos.map((video) => (
                  <figure key={video.id}>
                    <video controls preload="none" playsInline src={video.url} aria-label={video.description || video.originalFileName} />
                    <figcaption>{video.description || video.originalFileName}</figcaption>
                  </figure>
                ))}
              </div>
            </section>
          )}

          <section className="tenant-room-actions">
            <div><strong>Hợp đồng thuê</strong><span>Ngày bắt đầu {formatDate(room.leaseStartsOn)} · {formatDate(room.leaseEndsOn)}</span></div>
            <Link to="/tenant/dashboard">Thông tin tài khoản <MoveUpRight size={15} /></Link>
          </section>
        </>
      ) : (
        <section className="tenant-room-empty">
          <span className="tenant-room-empty-icon"><Search size={23} /></span>
          <h2>Bạn hiện chưa thuê phòng nào</h2>
          <p>Khi có hợp đồng đang hiệu lực được liên kết với tài khoản này, thông tin phòng sẽ hiển thị tại đây.</p>
          <Link to="/tenant/dat-phong">Tìm phòng trọ</Link>
        </section>
      )}

      {lightboxOpen && activeImage && (
        <div className="tenant-room-lightbox" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setLightboxOpen(false) }}>
          <button className="tenant-room-lightbox-close" type="button" onClick={() => setLightboxOpen(false)} aria-label="Đóng ảnh"><X size={21} /></button>
          {images.length > 1 && <button className="tenant-room-lightbox-arrow previous" type="button" onClick={() => moveImage(-1)} aria-label="Ảnh trước"><ArrowLeft /></button>}
          <figure><img src={activeImage.url} alt={activeImage.description || activeImage.originalFileName} /><figcaption>{activeImage.description || activeImage.originalFileName}</figcaption></figure>
          {images.length > 1 && <button className="tenant-room-lightbox-arrow next" type="button" onClick={() => moveImage(1)} aria-label="Ảnh tiếp theo"><ArrowRight /></button>}
        </div>
      )}
    </div>
  )
}
