import { useEffect, useState } from 'react'
import { ChevronLeft, ChevronRight, X } from 'lucide-react'
import { ApiError, apiRequest } from '../auth/api'
import { useAuth } from '../auth/useAuth'
import './room-media.css'

export type RoomMedia = {
  id: number
  roomId: number
  mediaType: 'IMAGE' | 'VIDEO'
  originalFileName: string
  storedFileName: string
  url: string
  mimeType: string
  sizeBytes: number
  displayOrder: number
  primaryImage: boolean
  description: string | null
  uploadedAt: string
  uploadedBy: number | null
}

export default function RoomMediaGallery({ roomId, roomName, onClose }: {
  roomId: number
  roomName: string
  onClose: () => void
}) {
  const { token } = useAuth()
  const [media, setMedia] = useState<RoomMedia[]>([])
  const [selectedImage, setSelectedImage] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    void apiRequest<RoomMedia[]>(`/rooms/${roomId}/media`, { signal: controller.signal }, token ?? undefined)
      .then(setMedia)
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) setError(cause instanceof ApiError ? cause.message : 'Không tải được thư viện phòng.')
      })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [roomId, token])

  const images = media.filter((item) => item.mediaType === 'IMAGE')
  const videos = media.filter((item) => item.mediaType === 'VIDEO')
  const selected = selectedImage === null ? null : images[selectedImage]

  function moveSelection(direction: -1 | 1) {
    if (images.length < 1 || selectedImage === null) return
    setSelectedImage((selectedImage + direction + images.length) % images.length)
  }

  return (
    <div className="media-overlay" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose()
    }}>
      <section className="media-dialog" role="dialog" aria-modal="true" aria-labelledby="room-media-title">
        <header className="media-dialog-heading">
          <div><span className="media-eyebrow">THƯ VIỆN PHÒNG</span><h2 id="room-media-title">{roomName}</h2></div>
          <button type="button" className="media-icon-button" onClick={onClose} aria-label="Đóng thư viện"><X size={20} /></button>
        </header>
        {error && <div className="media-alert" role="alert">{error}</div>}
        {loading ? <p className="media-empty">Đang tải thư viện…</p> : (
          <>
            <section className="media-gallery-section">
              <h3>Ảnh ({images.length})</h3>
              {images.length === 0 ? <p className="media-empty">Phòng chưa có ảnh.</p> : (
                <div className="media-image-grid">
                  {images.map((image, index) => (
                    <button className="media-image-tile" type="button" key={image.id} onClick={() => setSelectedImage(index)}>
                      <img src={image.url} alt={image.description || image.originalFileName} loading="lazy" />
                      {image.description && <span>{image.description}</span>}
                    </button>
                  ))}
                </div>
              )}
            </section>
            <section className="media-gallery-section">
              <h3>Video ({videos.length})</h3>
              {videos.length === 0 ? <p className="media-empty">Phòng chưa có video.</p> : (
                <div className="media-video-list">
                  {videos.map((video) => (
                    <figure key={video.id} className="media-video-item">
                      <video controls preload="none" playsInline src={video.url} aria-label={video.description || video.originalFileName} />
                      <figcaption>{video.description || video.originalFileName}</figcaption>
                    </figure>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </section>

      {selected && (
        <div className="media-lightbox" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget) setSelectedImage(null)
        }}>
          <button type="button" className="media-lightbox-close" onClick={() => setSelectedImage(null)} aria-label="Đóng ảnh"><X /></button>
          <button type="button" className="media-lightbox-arrow previous" onClick={() => moveSelection(-1)} aria-label="Ảnh trước"><ChevronLeft /></button>
          <figure>
            <img src={selected.url} alt={selected.description || selected.originalFileName} />
            <figcaption>{selected.description || selected.originalFileName}</figcaption>
          </figure>
          <button type="button" className="media-lightbox-arrow next" onClick={() => moveSelection(1)} aria-label="Ảnh tiếp theo"><ChevronRight /></button>
        </div>
      )}
    </div>
  )
}
