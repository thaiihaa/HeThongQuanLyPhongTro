import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, apiRequest } from '../auth/api'
import { useAuth } from '../auth/useAuth'
import RoomMediaGallery from '../media/RoomMediaGallery'
import './booking.css'

type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

type Room = {
  id: number
  roomCode: string
  siteName: string
  address: string
  roomType: string
  areaSquareMeters: number
  monthlyRent: number
  floor: string | null
  description: string | null
  primaryImageUrl: string | null
}

type TenantBooking = {
  id: number
  roomCode: string
  siteName: string
  expectedCheckIn: string
  expectedCheckOut: string | null
  status: string
  note: string | null
  createdAt: string
}

type AdminBooking = TenantBooking & {
  tenantName: string
  phone: string
  email: string | null
  planCode: 'THUONG' | 'PREMIUM'
}

const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
const localToday = new Date(Date.now() - new Date().getTimezoneOffset() * 60_000).toISOString().slice(0, 10)
const statusNames: Record<string, string> = {
  CHO_XAC_NHAN: 'Chờ xác nhận',
  DA_XAC_NHAN: 'Đã xác nhận',
  DA_HUY: 'Đã hủy',
  HOAN_TAT: 'Hoàn tất',
}

export function RoomsPage() {
  const { token, user } = useAuth()
  const [rooms, setRooms] = useState<Room[]>([])
  const [bookings, setBookings] = useState<TenantBooking[]>([])
  const [selectedRoom, setSelectedRoom] = useState<Room | null>(null)
  const [galleryRoom, setGalleryRoom] = useState<Room | null>(null)
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [roomError, setRoomError] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [reload, setReload] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    const query = new URLSearchParams({ page: '0', size: '100' })
    if (search.trim()) query.set('search', search.trim())
    void apiRequest<PageResponse<Room>>(`/rooms?${query}`, { signal: controller.signal })
      .then((result) => setRooms(result.content))
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setRoomError(cause instanceof ApiError ? cause.message : 'Không tải được danh sách phòng.')
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [search, reload])

  useEffect(() => {
    if (user?.role !== 'NGUOI_THUE' || !token) return
    const controller = new AbortController()
    void apiRequest<PageResponse<TenantBooking>>('/tenant/dat-phong?page=0&size=20', {
      signal: controller.signal,
    }, token).then((result) => setBookings(result.content))
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(cause instanceof ApiError ? cause.message : 'Không tải được yêu cầu đặt phòng.')
      })
    return () => controller.abort()
  }, [reload, token, user?.role])

  async function submitBooking(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedRoom || !token) return
    setSubmitting(true)
    setError('')
    setNotice('')
    const form = new FormData(event.currentTarget)
    try {
      const response = await apiRequest<{ id: number; status: string }>('/tenant/dat-phong', {
        method: 'POST',
        body: JSON.stringify({
          roomId: selectedRoom.id,
          phone: form.get('phone'),
          email: form.get('email') || null,
          expectedCheckIn: form.get('expectedCheckIn'),
          expectedCheckOut: form.get('expectedCheckOut') || null,
          note: form.get('note') || null,
        }),
      }, token)
      setNotice(`Đã gửi yêu cầu #${response.id}. Trạng thái: ${statusNames[response.status] ?? response.status}.`)
      setSelectedRoom(null)
      setReload((value) => value + 1)
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Không gửi được yêu cầu đặt phòng.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="booking-page">
      <header className="booking-heading">
        <div>
          <span className="booking-eyebrow">TÌM NƠI Ở PHÙ HỢP</span>
          <h1>Phòng đang trống</h1>
          <p>Xem thông tin phòng và gửi yêu cầu đặt phòng trực tuyến.</p>
        </div>
        <label className="booking-search">
          <span>Tìm phòng</span>
          <input value={search} onChange={(event) => { setSearch(event.target.value); setRoomError('') }} placeholder="Mã phòng, khu trọ, địa chỉ..." />
        </label>
      </header>

      {roomError && <div className="booking-alert error" role="alert">{roomError}</div>}
      {notice && <div className="booking-alert success" role="status">{notice}</div>}
      {error && <div className="booking-alert error" role="alert">{error}</div>}

      {selectedRoom && (
        <form className="booking-form" onSubmit={(event) => void submitBooking(event)}>
          <div className="booking-form-heading">
            <div><span className="booking-eyebrow">GỬI YÊU CẦU</span><h2>{selectedRoom.roomCode} · {selectedRoom.siteName}</h2></div>
            <button className="booking-close" type="button" onClick={() => setSelectedRoom(null)} aria-label="Đóng biểu mẫu">×</button>
          </div>
          <div className="booking-form-grid">
            <label>Số điện thoại<input name="phone" required pattern="0[0-9]{9,10}" title="Nhập số điện thoại Việt Nam bắt đầu bằng số 0." /></label>
            <label>Email (không bắt buộc)<input name="email" type="email" maxLength={254} /></label>
            <label>Ngày nhận phòng<input name="expectedCheckIn" type="date" required min={localToday} /></label>
            <label>Ngày trả phòng (không bắt buộc)<input name="expectedCheckOut" type="date" /></label>
            <label className="booking-note">Ghi chú<textarea name="note" rows={3} maxLength={1000} /></label>
          </div>
          <button className="booking-primary" type="submit" disabled={submitting}>{submitting ? 'Đang gửi...' : 'Gửi yêu cầu đặt phòng'}</button>
        </form>
      )}

      {loading ? <p className="booking-empty">Đang tải danh sách phòng...</p> : roomError ? null : rooms.length === 0
        ? <p className="booking-empty">Hiện không có phòng phù hợp với tìm kiếm.</p>
        : <div className="room-grid">{rooms.map((room) => (
          <article className="room-card" key={room.id}>
            <img className="room-card-image" src={room.primaryImageUrl || '/room-placeholder.svg'}
              alt={`Ảnh phòng ${room.roomCode}`} loading="lazy"
              onError={(event) => { event.currentTarget.src = '/room-placeholder.svg' }} />
            <div className="room-card-top"><span className="room-type">{room.roomType}</span><span className="room-vacancy">Đang trống</span></div>
            <h2>{room.roomCode}</h2>
            <p className="room-site">{room.siteName}</p>
            <p className="room-address">{room.address}</p>
            <div className="room-facts">
              <span>{room.areaSquareMeters} m²</span>
              {room.floor && <span>Tầng {room.floor}</span>}
            </div>
            {room.description && <p className="room-description">{room.description}</p>}
            <button type="button" className="room-gallery-button" onClick={() => setGalleryRoom(room)}>Xem ảnh và video</button>
            <div className="room-card-bottom">
              <strong>{money.format(room.monthlyRent)}<small>/tháng</small></strong>
              {user?.role === 'NGUOI_THUE'
                ? <button type="button" className="booking-primary" onClick={() => { setError(''); setSelectedRoom(room) }}>Đặt phòng</button>
                : user
                  ? <span className="room-readonly">Chỉ người thuê mới có thể đặt phòng</span>
                  : <Link className="booking-primary" to="/login">Đăng nhập để đặt</Link>}
            </div>
          </article>
        ))}</div>}

      {galleryRoom && <RoomMediaGallery roomId={galleryRoom.id} roomName={`${galleryRoom.roomCode} · ${galleryRoom.siteName}`}
        onClose={() => setGalleryRoom(null)} />}

      {user?.role === 'NGUOI_THUE' && (
        <section className="tenant-bookings">
          <div className="booking-section-heading"><div><span className="booking-eyebrow">THEO DÕI YÊU CẦU</span><h2>Yêu cầu đặt phòng của tôi</h2></div></div>
          {bookings.length === 0 ? <p className="booking-empty">Bạn chưa gửi yêu cầu đặt phòng.</p> : (
            <div className="booking-table-wrap"><table className="booking-table">
              <thead><tr><th>Phòng</th><th>Ngày nhận</th><th>Ngày trả</th><th>Trạng thái</th><th>Ngày gửi</th></tr></thead>
              <tbody>{bookings.map((booking) => <tr key={booking.id}>
                <td><strong>{booking.roomCode}</strong><small>{booking.siteName}</small></td>
                <td>{booking.expectedCheckIn}</td><td>{booking.expectedCheckOut ?? 'Chưa xác định'}</td>
                <td><span className={`booking-status status-${booking.status.toLowerCase()}`}>{statusNames[booking.status] ?? booking.status}</span></td>
                <td>{new Date(booking.createdAt).toLocaleDateString('vi-VN')}</td>
              </tr>)}</tbody>
            </table></div>
          )}
        </section>
      )}
    </section>
  )
}

export function AdminBookingPage() {
  const { token } = useAuth()
  const [bookings, setBookings] = useState<AdminBooking[]>([])
  const [status, setStatus] = useState('')
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [reload, setReload] = useState(0)

  useEffect(() => {
    if (!token) return
    const controller = new AbortController()
    const query = new URLSearchParams({ page: '0', size: '100' })
    if (status) query.set('status', status)
    void apiRequest<PageResponse<AdminBooking>>(`/admin/dat-phong?${query}`, {
      signal: controller.signal,
    }, token).then((result) => setBookings(result.content))
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(cause instanceof ApiError ? cause.message : 'Không tải được yêu cầu đặt phòng.')
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [reload, status, token])

  async function changeStatus(id: number, nextStatus: string) {
    if (!token) return
    setSavingId(id)
    setError('')
    setNotice('')
    try {
      await apiRequest<null>(`/admin/dat-phong/${id}/status`, {
        method: 'PUT',
        body: JSON.stringify({ status: nextStatus }),
      }, token)
      setNotice(`Đã cập nhật yêu cầu #${id}.`)
      setReload((value) => value + 1)
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Không cập nhật được trạng thái.')
    } finally {
      setSavingId(null)
    }
  }

  return (
    <section className="booking-page">
      <header className="booking-heading">
        <div><span className="booking-eyebrow">QUẢN LÝ ĐẶT PHÒNG</span><h1>Yêu cầu đặt phòng</h1><p>Xem thông tin liên hệ và xử lý các yêu cầu của người thuê.</p></div>
        <label className="booking-search"><span>Lọc trạng thái</span>
          <select value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="">Tất cả trạng thái</option>
            {Object.entries(statusNames).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
      </header>
      {notice && <div className="booking-alert success" role="status">{notice}</div>}
      {error && <div className="booking-alert error" role="alert">{error}</div>}
      {loading ? <p className="booking-empty">Đang tải yêu cầu...</p> : bookings.length === 0
        ? <p className="booking-empty">Không có yêu cầu đặt phòng ở trạng thái này.</p>
        : <div className="booking-table-wrap"><table className="booking-table admin-booking-table">
          <thead><tr><th>Phòng</th><th>Người đặt</th><th>Liên hệ</th><th>Gói</th><th>Thời gian</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
          <tbody>{bookings.map((booking) => <tr key={booking.id}>
            <td><strong>{booking.roomCode}</strong><small>{booking.siteName}</small></td>
            <td>{booking.tenantName}</td>
            <td>{booking.phone}<small>{booking.email ?? 'Không cung cấp email'}</small></td>
            <td><span className={`booking-plan ${booking.planCode === 'PREMIUM' ? 'premium' : ''}`}>{booking.planCode === 'PREMIUM' ? 'Premium · ưu tiên' : 'Thường'}</span></td>
            <td>{booking.expectedCheckIn}<small>Đến: {booking.expectedCheckOut ?? 'chưa xác định'}</small></td>
            <td><span className={`booking-status status-${booking.status.toLowerCase()}`}>{statusNames[booking.status] ?? booking.status}</span></td>
            <td><div className="booking-actions">
              {booking.status === 'CHO_XAC_NHAN' && <>
                <button type="button" className="booking-action approve" disabled={savingId === booking.id} onClick={() => void changeStatus(booking.id, 'DA_XAC_NHAN')}>Xác nhận</button>
                <button type="button" className="booking-action reject" disabled={savingId === booking.id} onClick={() => void changeStatus(booking.id, 'DA_HUY')}>Từ chối</button>
              </>}
              {booking.status === 'DA_XAC_NHAN' && <>
                <button type="button" className="booking-action reject" disabled={savingId === booking.id} onClick={() => void changeStatus(booking.id, 'DA_HUY')}>Hủy</button>
                <button type="button" className="booking-action complete" disabled={savingId === booking.id} onClick={() => void changeStatus(booking.id, 'HOAN_TAT')}>Hoàn tất</button>
              </>}
            </div></td>
          </tr>)}</tbody>
        </table></div>}
    </section>
  )
}
