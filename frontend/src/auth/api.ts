export type User = {
  id: number
  username: string
  fullName: string
  role: 'ADMIN' | 'QUAN_LY' | 'NHAN_VIEN' | 'NGUOI_THUE'
}

export type AuthResponse = {
  accessToken: string
  tokenType: 'Bearer'
  expiresAt: string
  user: User
}

export type TenantDashboard = {
  userId: number
  username: string
  fullName: string
  email: string | null
  phone: string | null
  role: string
  citizenId: string | null
  birthDate: string | null
  address: string | null
}

export type TenantPlan = {
  code: 'THUONG' | 'PREMIUM'
  name: string
  startsOn: string | null
  expiresOn: string | null
  premium: boolean
  benefits: string
}

export type TenantRoom = {
  roomId: number
  roomCode: string
  siteName: string
  address: string
  areaSquareMeters: number
  monthlyRent: number
  status: string
  description: string | null
  leaseStartsOn: string
  leaseEndsOn: string | null
  amenities: Array<{ name: string; quantity: number; condition: string | null }>
  assets: Array<{ name: string; quantity: number; condition: string | null }>
  services: Array<{ name: string; unit: string; appliedPrice: number; calculationMethod: string }>
  media: Array<{
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
  }>
}

export type AdminDashboard = {
  totalAccounts: number
  activeAccounts: number
  lockedAccounts: number
  adminAccounts: number
  tenantAccounts: number
  totalSites: number
  totalRooms: number
  vacantRooms: number
  occupiedRooms: number
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

export async function apiRequest<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  let response: Response
  try {
    response = await fetch(`/api/v1${path}`, { ...init, headers })
  } catch {
    throw new ApiError(0, 'API_UNREACHABLE', 'Không thể kết nối tới máy chủ. Vui lòng thử lại.')
  }

  const payload: unknown = await response.json().catch(() => null)
  if (!response.ok) {
    if (response.status === 401 && token && typeof window !== 'undefined') {
      window.dispatchEvent(new Event('qlpt:session-invalid'))
    }
    const body = typeof payload === 'object' && payload !== null ? payload as Record<string, unknown> : {}
    const message = typeof body.message === 'string' ? body.message : 'Yêu cầu không thành công.'
    const code = typeof body.code === 'string' ? body.code : 'REQUEST_FAILED'
    throw new ApiError(response.status, code, message)
  }
  return payload as T
}
