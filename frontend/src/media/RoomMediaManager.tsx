import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { ArrowDown, ArrowUp, ImagePlus, Star, Trash2, Upload, Video } from 'lucide-react'
import { ApiError, apiRequest } from '../auth/api'
import { useAuth } from '../auth/useAuth'
import type { RoomMedia } from './RoomMediaGallery'
import './room-media.css'

export default function RoomMediaManager({ roomId, roomName }: { roomId: number; roomName: string }) {
  const { token } = useAuth()
  const [media, setMedia] = useState<RoomMedia[]>([])
  const [files, setFiles] = useState<Array<{ file: File; previewUrl: string }>>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [reload, setReload] = useState(0)
  const previewUrls = useRef(new Set<string>())

  useEffect(() => {
    const activeUrls = previewUrls.current
    return () => activeUrls.forEach((url) => URL.revokeObjectURL(url))
  }, [])

  useEffect(() => {
    if (!token) return
    const controller = new AbortController()
    void apiRequest<RoomMedia[]>(`/admin/phong-tro/${roomId}/media`, { signal: controller.signal }, token)
      .then(setMedia)
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(cause instanceof ApiError ? cause.message : 'Không tải được media của phòng.')
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [reload, roomId, token])

  function selectFiles(event: ChangeEvent<HTMLInputElement>) {
    setError('')
    setNotice('')
    const selected = Array.from(event.target.files ?? []).map((file) => {
      const previewUrl = URL.createObjectURL(file)
      previewUrls.current.add(previewUrl)
      return { file, previewUrl }
    })
    setFiles((current) => [...current, ...selected])
    event.target.value = ''
  }

  async function uploadSelected() {
    if (!token || files.length === 0) return
    setUploading(true)
    setError('')
    setNotice('')
    const body = new FormData()
    files.forEach(({ file }) => body.append('files', file))
    try {
      const response = await fetch(`/api/v1/admin/phong-tro/${roomId}/media`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
        body,
      })
      if (!response.ok) {
        const payload: unknown = await response.json().catch(() => null)
        const message = typeof payload === 'object' && payload !== null && 'message' in payload
          ? String(payload.message)
          : 'Không tải được tệp lên.'
        throw new ApiError(response.status, 'UPLOAD_FAILED', message)
      }
      const uploaded = await response.json() as RoomMedia[]
      files.forEach(({ previewUrl }) => {
        URL.revokeObjectURL(previewUrl)
        previewUrls.current.delete(previewUrl)
      })
      setFiles([])
      setNotice(`Đã tải lên ${uploaded.length} tệp.`)
      setReload((value) => value + 1)
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Không kết nối được máy chủ để tải tệp.')
    } finally {
      setUploading(false)
    }
  }

  async function updateMedia(item: RoomMedia, action: 'primary' | 'delete' | 'order' | 'description', value?: number | string) {
    if (!token) return
    setError('')
    setNotice('')
    const endpoint = `/admin/phong-tro/${roomId}/media/${item.id}`
    try {
      if (action === 'delete') {
        if (!window.confirm(`Xóa tệp "${item.originalFileName}"?`)) return
        await apiRequest<null>(endpoint, { method: 'DELETE' }, token)
      } else {
        const suffix = action === 'primary' ? '/primary' : `/${action}`
        const body = action === 'order' ? { displayOrder: value }
          : action === 'description' ? { description: value } : undefined
        await apiRequest<null>(`${endpoint}${suffix}`, {
          method: 'PUT',
          ...(body ? { body: JSON.stringify(body) } : {}),
        }, token)
      }
      setNotice(action === 'delete' ? 'Đã xóa tệp.' : 'Đã cập nhật media.')
      setReload((current) => current + 1)
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Không cập nhật được media.')
    }
  }

  return (
    <section className="media-manager" aria-labelledby="media-manager-title">
      <div className="media-manager-heading">
        <div><span className="media-eyebrow">ẢNH VÀ VIDEO THỰC TẾ</span><h3 id="media-manager-title">{roomName}</h3>
          <p>Ảnh tối đa 20 MB/tệp; video tối đa 500 MB/tệp; tối đa 10 tệp/lần.</p></div>
      </div>
      {error && <div className="media-alert" role="alert">{error}</div>}
      {notice && <div className="media-notice" role="status">{notice}</div>}
      <div className="media-upload-controls">
        <label className="media-upload-picker"><ImagePlus size={16} /> Chọn ảnh
          <input type="file" accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp" multiple onChange={selectFiles} />
        </label>
        <label className="media-upload-picker"><Video size={16} /> Chọn video
          <input type="file" accept=".mp4,.webm,video/mp4,video/webm" multiple onChange={selectFiles} />
        </label>
        <button type="button" className="media-upload-button" disabled={uploading || files.length === 0} onClick={() => void uploadSelected()}>
          <Upload size={15} />{uploading ? 'Đang tải lên…' : `Tải lên ${files.length || ''} tệp`}
        </button>
      </div>
      {files.length > 0 && <div className="media-pending-grid">
        {files.map(({ file, previewUrl }, index) => (
          <figure className="media-pending-item" key={`${file.name}-${index}`}>
            {file.type.startsWith('image/')
              ? <img src={previewUrl} alt={`Xem trước ${file.name}`} />
              : <video src={previewUrl} preload="metadata" controls />}
            <figcaption>{file.name} · {formatBytes(file.size)}</figcaption>
            <button type="button" onClick={() => {
              URL.revokeObjectURL(previewUrl)
              previewUrls.current.delete(previewUrl)
              setFiles((current) => current.filter((_, fileIndex) => fileIndex !== index))
            }}>Bỏ tệp</button>
          </figure>
        ))}
      </div>}
      <h4 className="media-list-title">Media đã tải</h4>
      {loading ? <p className="media-empty">Đang tải…</p> : media.length === 0 ? <p className="media-empty">Chưa có ảnh hoặc video.</p> : (
        <div className="media-managed-list">
          {media.map((item, index) => (
            <article className="media-managed-item" key={item.id}>
              {item.mediaType === 'IMAGE'
                ? <img className="media-managed-preview" src={item.url} alt={item.description || item.originalFileName} loading="lazy" />
                : <video className="media-managed-preview" src={item.url} preload="none" controls />}
              <div className="media-managed-details">
                <strong>{item.originalFileName}</strong>
                <span>{item.mediaType === 'IMAGE' ? 'Ảnh' : 'Video'} · {formatBytes(item.sizeBytes)}</span>
                <label>Mô tả
                  <input defaultValue={item.description ?? ''} maxLength={500}
                    onBlur={(event) => {
                      const next = event.currentTarget.value.trim()
                      if (next !== (item.description ?? '')) void updateMedia(item, 'description', next)
                    }} />
                </label>
              </div>
              <div className="media-managed-actions">
                {item.mediaType === 'IMAGE' && <button type="button" title="Đặt làm ảnh đại diện" aria-label={`Đặt ${item.originalFileName} làm ảnh đại diện`} onClick={() => void updateMedia(item, 'primary')}>
                  <Star size={16} fill={item.primaryImage ? 'currentColor' : 'none'} />{item.primaryImage && <span>Đại diện</span>}
                </button>}
                <button type="button" title="Lên thứ tự" aria-label="Tăng thứ tự hiển thị" disabled={index === 0} onClick={() => void updateMedia(item, 'order', media[index - 1].displayOrder)}><ArrowUp size={16} /></button>
                <button type="button" title="Xuống thứ tự" aria-label="Giảm thứ tự hiển thị" disabled={index === media.length - 1} onClick={() => void updateMedia(item, 'order', media[index + 1].displayOrder)}><ArrowDown size={16} /></button>
                <button type="button" className="media-delete-button" title="Xóa tệp" aria-label={`Xóa ${item.originalFileName}`} onClick={() => void updateMedia(item, 'delete')}><Trash2 size={16} /></button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

function formatBytes(bytes: number) {
  return bytes >= 1024 * 1024 ? `${(bytes / (1024 * 1024)).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`
}
