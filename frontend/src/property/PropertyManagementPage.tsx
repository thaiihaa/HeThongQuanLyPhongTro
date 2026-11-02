import { useEffect, useState, type FormEvent } from 'react'
import { CircleAlert, Pencil, Plus, RefreshCw, Search, X } from 'lucide-react'
import { ApiError, apiRequest } from '../auth/api'
import { useAuth } from '../auth/useAuth'
import RoomMediaManager from '../media/RoomMediaManager'
import './property-management.css'

type Kind = 'khu-tro' | 'loai-phong' | 'phong-tro'

type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

type KhuTro = {
  id: number
  maKhuTro: string
  tenKhuTro: string
  diaChi: string
  moTa: string | null
  trangThai: string
}

type LoaiPhong = {
  id: number
  maLoaiPhong: string
  tenLoaiPhong: string
  dienTichM2: number
  giaThueMacDinh: number
  moTa: string | null
  trangThai: string
}

type PhongTro = {
  id: number
  khuTroId: number
  tenKhuTro: string
  loaiPhongId: number
  tenLoaiPhong: string
  maPhong: string
  tang: string | null
  dienTichM2: number
  giaThue: number
  trangThai: string
  moTa: string | null
}

type PropertyRow = {
  id: number
  code: string
  name: string
  detail: string
  price: number | null
  status: string
  form: FormValues
}

type FormValues = {
  code: string
  name: string
  address: string
  description: string
  status: string
  area: string
  price: string
  siteId: string
  roomTypeId: string
  floor: string
}

const emptyForm: FormValues = {
  code: '',
  name: '',
  address: '',
  description: '',
  status: '',
  area: '',
  price: '',
  siteId: '',
  roomTypeId: '',
  floor: '',
}

const pages: Record<Kind, { title: string; description: string; endpoint: string; singular: string }> = {
  'khu-tro': {
    title: 'Quản lý khu trọ',
    description: 'Tạo và cập nhật thông tin các khu trọ trong hệ thống.',
    endpoint: '/admin/khu-tro',
    singular: 'khu trọ',
  },
  'loai-phong': {
    title: 'Quản lý loại phòng',
    description: 'Thiết lập diện tích và giá thuê mặc định theo loại phòng.',
    endpoint: '/admin/loai-phong',
    singular: 'loại phòng',
  },
  'phong-tro': {
    title: 'Quản lý phòng trọ',
    description: 'Theo dõi khu, loại phòng, diện tích, giá thuê và trạng thái phòng.',
    endpoint: '/admin/phong-tro',
    singular: 'phòng',
  },
}

const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })

export default function PropertyManagementPage({ kind }: { kind: Kind }) {
  const { token } = useAuth()
  const config = pages[kind]
  const [rows, setRows] = useState<PropertyRow[]>([])
  const [sites, setSites] = useState<KhuTro[]>([])
  const [roomTypes, setRoomTypes] = useState<LoaiPhong[]>([])
  const [form, setForm] = useState<FormValues>(emptyForm)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [reload, setReload] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    const query = new URLSearchParams({ page: String(page), size: '20' })
    if (search) query.set('search', search)
    void apiRequest<PageResponse<KhuTro | LoaiPhong | PhongTro>>(
      `${config.endpoint}?${query.toString()}`,
      { signal: controller.signal },
      token ?? undefined,
    ).then((result) => {
      if (!active) return
      setRows(result.content.map((item) => toRow(kind, item)))
      setTotalPages(result.totalPages)
      setTotalElements(result.totalElements)
    }).catch((cause: unknown) => {
      if (active) setError(cause instanceof ApiError ? cause.message : `Không tải được danh sách ${config.singular}.`)
    }).finally(() => {
      if (active) setLoading(false)
    })

    return () => { active = false; controller.abort() }
  }, [config.endpoint, config.singular, kind, page, reload, search, token])

  useEffect(() => {
    if (kind !== 'phong-tro') return
    const controller = new AbortController()
    void Promise.all([
      apiRequest<PageResponse<KhuTro>>('/admin/khu-tro?page=0&size=100', { signal: controller.signal }, token ?? undefined),
      apiRequest<PageResponse<LoaiPhong>>('/admin/loai-phong?page=0&size=100', { signal: controller.signal }, token ?? undefined),
    ]).then(([siteResult, typeResult]) => {
      setSites(siteResult.content)
      setRoomTypes(typeResult.content)
    }).catch((cause: unknown) => {
      if (!controller.signal.aborted) setError(cause instanceof ApiError ? cause.message : 'Không tải được danh mục khu và loại phòng.')
    })
    return () => controller.abort()
  }, [kind, reload, token])

  function startCreate() {
    setEditingId(null)
    setForm({ ...emptyForm, status: kind === 'phong-tro' ? 'TRONG' : 'HOAT_DONG' })
    setNotice('')
    setError('')
  }

  function startEdit(row: PropertyRow) {
    setEditingId(row.id)
    setForm(row.form)
    setNotice('')
    setError('')
  }

  function cancelEdit() {
    setEditingId(null)
    setForm(emptyForm)
  }

  function updateForm(field: keyof FormValues, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  async function submitForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setNotice('')

    const payload = buildPayload(kind, form)
    try {
      await apiRequest(`${config.endpoint}${editingId === null ? '' : `/${editingId}`}`, {
        method: editingId === null ? 'POST' : 'PUT',
        body: JSON.stringify(payload),
      }, token ?? undefined)
      setNotice(editingId === null ? `Đã thêm ${config.singular}.` : 'Đã cập nhật thông tin.')
      setEditingId(null)
      setForm(emptyForm)
      setLoading(true)
      setReload((value) => value + 1)
    } catch (cause: unknown) {
      setError(cause instanceof ApiError ? cause.message : 'Không lưu được dữ liệu. Vui lòng thử lại.')
    } finally {
      setSaving(false)
    }
  }

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setPage(0)
    setSearch(searchInput.trim())
    setReload((value) => value + 1)
  }

  function refreshList() {
    setLoading(true)
    setError('')
    setReload((value) => value + 1)
  }

  function changePage(nextPage: number) {
    setLoading(true)
    setPage(nextPage)
  }

  return (
    <div className="work-page property-page">
      <section className="work-heading">
        <div><span className="work-eyebrow">QUẢN TRỊ / DANH MỤC</span><h1>{config.title}</h1><p>{config.description}</p></div>
        <button className="refresh-button" type="button" onClick={refreshList} disabled={loading}>
          <RefreshCw size={16} className={loading ? 'spin' : ''} /><span>Tải lại</span>
        </button>
      </section>

      {error && <div className="dashboard-error" role="alert"><CircleAlert size={18} /><span>{error}</span></div>}
      {notice && <div className="property-success" role="status">{notice}</div>}

      <section className="property-panel">
        <div className="property-toolbar">
          <div><strong>{totalElements}</strong><span> bản ghi</span></div>
          <div className="property-actions">
            <form className="property-search" onSubmit={submitSearch}>
              <Search size={16} />
              <input aria-label={`Tìm kiếm ${config.singular}`} value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Tìm theo tên hoặc mã…" />
              <button type="submit">Tìm</button>
            </form>
            <button className="property-primary" type="button" onClick={startCreate}><Plus size={16} /> Thêm {config.singular}</button>
          </div>
        </div>

        {(editingId !== null || form.status !== '') && (
          <form className="property-form" onSubmit={(event) => void submitForm(event)}>
            <div className="property-form-heading">
              <h2>{editingId === null ? `Thêm ${config.singular}` : `Cập nhật ${config.singular}`}</h2>
              <button type="button" className="property-icon-button" onClick={cancelEdit} aria-label="Đóng biểu mẫu"><X size={17} /></button>
            </div>
            <div className="property-form-grid">
              <Field label="Mã" value={form.code} onChange={(value) => updateForm('code', value)} required maxLength={30} />
              {kind !== 'phong-tro' && <Field label="Tên" value={form.name} onChange={(value) => updateForm('name', value)} required maxLength={kind === 'khu-tro' ? 150 : 120} />}
              {kind === 'khu-tro' && <Field label="Địa chỉ" value={form.address} onChange={(value) => updateForm('address', value)} required maxLength={500} />}
              {kind === 'phong-tro' && <>
                <label className="property-field"><span>Khu trọ</span><select value={form.siteId} onChange={(event) => updateForm('siteId', event.target.value)} required>
                  <option value="">Chọn khu trọ</option>{sites.map((site) => <option key={site.id} value={site.id}>{site.tenKhuTro}</option>)}
                </select></label>
                <label className="property-field"><span>Loại phòng</span><select value={form.roomTypeId} onChange={(event) => updateForm('roomTypeId', event.target.value)} required>
                  <option value="">Chọn loại phòng</option>{roomTypes.map((type) => <option key={type.id} value={type.id}>{type.tenLoaiPhong}</option>)}
                </select></label>
                <Field label="Tầng" value={form.floor} onChange={(value) => updateForm('floor', value)} maxLength={20} />
              </>}
              {kind !== 'khu-tro' && <>
                <Field label="Diện tích (m²)" value={form.area} onChange={(value) => updateForm('area', value)} type="number" min="0.01" step="0.01" required />
                <Field label={kind === 'loai-phong' ? 'Giá thuê mặc định (₫)' : 'Giá thuê (₫)'} value={form.price} onChange={(value) => updateForm('price', value)} type="number" min="0" step="1000" required />
              </>}
              {kind !== 'phong-tro' && <label className="property-field"><span>Trạng thái</span><select value={form.status} onChange={(event) => updateForm('status', event.target.value)}>
                <option value="HOAT_DONG">Đang hoạt động</option><option value="NGUNG_HOAT_DONG">Ngừng hoạt động</option>
              </select></label>}
              {kind === 'phong-tro' && <label className="property-field"><span>Trạng thái</span><select value={form.status} onChange={(event) => updateForm('status', event.target.value)}>
                <option value="TRONG">Còn trống</option><option value="DANG_THUE">Đang thuê</option><option value="DANG_SUA_CHUA">Đang sửa chữa</option><option value="KHONG_KHA_DUNG">Không khả dụng</option>
              </select></label>}
              {kind !== 'phong-tro' && <label className="property-field property-field-wide"><span>Mô tả</span><textarea value={form.description} onChange={(event) => updateForm('description', event.target.value)} maxLength={1000} rows={2} /></label>}
              {kind === 'phong-tro' && <label className="property-field property-field-wide"><span>Mô tả</span><textarea value={form.description} onChange={(event) => updateForm('description', event.target.value)} maxLength={1000} rows={2} /></label>}
            </div>
            {kind === 'phong-tro' && editingId !== null && (
              <RoomMediaManager roomId={editingId} roomName={form.code || `Phòng #${editingId}`} />
            )}
            <div className="property-form-actions">
              <button type="button" className="property-secondary" onClick={cancelEdit}>Hủy</button>
              <button type="submit" className="property-primary" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu thay đổi'}</button>
            </div>
          </form>
        )}

        {loading ? <div className="dashboard-empty">Đang tải dữ liệu…</div> : rows.length === 0 ? (
          <div className="dashboard-empty">{search ? 'Không tìm thấy kết quả phù hợp.' : `Chưa có ${config.singular} nào. Hãy thêm bản ghi đầu tiên.`}</div>
        ) : (
          <div className="property-table-wrap">
            <table className="property-table">
              <thead><tr><th>Mã</th><th>{kind === 'phong-tro' ? 'Phòng' : 'Tên'}</th><th>Thông tin</th><th>Giá thuê</th><th>Trạng thái</th><th><span className="sr-only">Thao tác</span></th></tr></thead>
              <tbody>{rows.map((row) => (
                <tr key={row.id}>
                  <td className="property-code">{row.code}</td><td className="property-name">{row.name}</td><td>{row.detail}</td>
                  <td>{row.price === null ? '—' : money.format(row.price)}</td>
                  <td><span className={`property-status ${row.status.toLowerCase()}`}>{statusLabel(row.status)}</span></td>
                  <td><button className="property-icon-button" type="button" onClick={() => startEdit(row)} aria-label={`Sửa ${row.name}`} title="Sửa"><Pencil size={15} /></button></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}

        <div className="property-pagination">
          <span>Trang {totalPages === 0 ? 0 : page + 1} / {totalPages}</span>
          <div><button type="button" className="property-secondary" onClick={() => changePage(Math.max(0, page - 1))} disabled={page === 0 || loading}>Trước</button>
            <button type="button" className="property-secondary" onClick={() => changePage(Math.min(totalPages - 1, page + 1))} disabled={page + 1 >= totalPages || loading}>Sau</button></div>
        </div>
      </section>
    </div>
  )
}

function Field({ label, value, onChange, required, maxLength, type = 'text', min, step }: {
  label: string
  value: string
  onChange: (value: string) => void
  required?: boolean
  maxLength?: number
  type?: string
  min?: string
  step?: string
}) {
  return <label className="property-field"><span>{label}</span><input type={type} value={value} onChange={(event) => onChange(event.target.value)} required={required} maxLength={maxLength} min={min} step={step} /></label>
}

function toRow(kind: Kind, item: KhuTro | LoaiPhong | PhongTro): PropertyRow {
  if (kind === 'khu-tro') {
    const site = item as KhuTro
    return {
      id: site.id,
      code: site.maKhuTro,
      name: site.tenKhuTro,
      detail: site.diaChi,
      price: null,
      status: site.trangThai,
      form: { ...emptyForm, code: site.maKhuTro, name: site.tenKhuTro, address: site.diaChi, description: site.moTa ?? '', status: site.trangThai },
    }
  }
  if (kind === 'loai-phong') {
    const type = item as LoaiPhong
    return {
      id: type.id,
      code: type.maLoaiPhong,
      name: type.tenLoaiPhong,
      detail: `${type.dienTichM2} m²`,
      price: type.giaThueMacDinh,
      status: type.trangThai,
      form: { ...emptyForm, code: type.maLoaiPhong, name: type.tenLoaiPhong, area: String(type.dienTichM2), price: String(type.giaThueMacDinh), description: type.moTa ?? '', status: type.trangThai },
    }
  }
  const room = item as PhongTro
  return {
    id: room.id,
    code: room.maPhong,
    name: room.tenLoaiPhong,
    detail: `${room.tenKhuTro} · ${room.dienTichM2} m²${room.tang ? ` · Tầng ${room.tang}` : ''}`,
    price: room.giaThue,
    status: room.trangThai,
    form: { ...emptyForm, code: room.maPhong, siteId: String(room.khuTroId), roomTypeId: String(room.loaiPhongId), floor: room.tang ?? '', area: String(room.dienTichM2), price: String(room.giaThue), status: room.trangThai, description: room.moTa ?? '' },
  }
}

function buildPayload(kind: Kind, values: FormValues) {
  if (kind === 'khu-tro') {
    return { maKhuTro: values.code, tenKhuTro: values.name, diaChi: values.address, moTa: values.description || null, trangThai: values.status }
  }
  if (kind === 'loai-phong') {
    return { maLoaiPhong: values.code, tenLoaiPhong: values.name, dienTichM2: Number(values.area), giaThueMacDinh: Number(values.price), moTa: values.description || null, trangThai: values.status }
  }
  return { khuTroId: Number(values.siteId), loaiPhongId: Number(values.roomTypeId), maPhong: values.code, tang: values.floor || null, dienTichM2: Number(values.area), giaThue: Number(values.price), trangThai: values.status, moTa: values.description || null }
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    HOAT_DONG: 'Hoạt động',
    NGUNG_HOAT_DONG: 'Ngừng hoạt động',
    TRONG: 'Còn trống',
    DANG_THUE: 'Đang thuê',
    DANG_SUA_CHUA: 'Đang sửa chữa',
    KHONG_KHA_DUNG: 'Không khả dụng',
  }
  return labels[status] ?? status
}
