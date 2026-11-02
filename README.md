# Hệ thống quản lý phòng trọ

Ứng dụng quản lý phòng trọ gồm website React/TypeScript/Vite, REST API Spring Boot và Microsoft SQL Server.

## Công nghệ

- Backend: Java 17+, Spring Boot 3.4.5, Maven, JDBC.
- Database: Microsoft SQL Server.
- Frontend: React, TypeScript, Vite.
- Xác thực: bearer session token; backend lưu hash token trong SQL Server và phân quyền bằng Spring Security.

## Cấu hình database

Tạo database `QuanLyPhongTro`, sau đó áp dụng các script theo thứ tự:

1. `database/004_authentication_schema.sql`
2. `database/005_login_sessions.sql`
3. `database/006_property_schema.sql`
4. `database/007_business_schema_draft.sql`
5. `database/008_tenant_plans.sql`
6. `database/009_room_media.sql`

`database/001_create_database.sql` chỉ cần dùng khi database chưa tồn tại. Các script là migration/schema, không chạy lại để reset dữ liệu. `database/010_demo_rooms.sql` là script dữ liệu demo tùy chọn; chỉ chạy trên database local/dev sau khi kiểm tra nội dung và tài khoản hiện có. Không chạy script demo trên production.

Không chạy `database/003_enable_tcp_for_jdbc.ps1` nếu chưa được DBA cho phép; script này thay đổi cấu hình SQL Server và khởi động lại dịch vụ.

Backend lấy kết nối từ biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; không commit thông tin xác thực. Sao chép `.env.example` để tham khảo cấu hình, nhưng Spring Boot không tự nạp file `.env`. Với Windows Integrated Authentication, cần driver native tương ứng với Microsoft JDBC Driver và JVM; xem hướng dẫn cục bộ/DBA để cấu hình môi trường.

## Chạy backend

Trong PowerShell ở thư mục gốc, đặt `DB_URL` trỏ tới T-SQL TCP listener thông thường của SQL Server (không dùng cổng DAC), rồi chạy:

```powershell
$env:DB_URL = "jdbc:sqlserver://<SQL_SERVER>:<TSQL_PORT>;databaseName=QuanLyPhongTro;encrypt=true;trustServerCertificate=true"
$env:DB_USERNAME = "<DATABASE_USER>"
$env:DB_PASSWORD = "<DATABASE_PASSWORD>"
$env:SERVER_PORT = "8082"
$env:JPA_DDL_AUTO = "none"
.\tools\apache-maven-3.9.6\bin\mvn.cmd -f backend\pom.xml spring-boot:run
```

Chỉ dùng `trustServerCertificate=true` cho môi trường phát triển tin cậy; production cần chứng thư máy chủ hợp lệ. Không lưu mật khẩu thật trong file hoặc commit.

Health check:

```text
GET http://localhost:8082/api/v1/health/database
```

## Chạy frontend

```powershell
cd frontend
npm install
$env:VITE_API_TARGET = "http://127.0.0.1:8082"
npm run dev
```

Website mặc định chạy tại `http://localhost:5173`. Có thể cấu hình proxy qua `frontend/.env.example`; file `.env` thật không được commit.

## Kiểm tra chất lượng

```powershell
.\tools\apache-maven-3.9.6\bin\mvn.cmd -f backend\pom.xml verify
cd frontend
npm run lint
npm run build
```

## API và quyền truy cập chính

- `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, `GET /api/v1/auth/me`.
- `GET /api/v1/rooms`: danh sách phòng trống.
- `GET /api/v1/tenant/dashboard`, `GET /api/v1/tenant/my-room`, `GET /api/v1/tenant/plan`: chỉ người thuê đã đăng nhập; endpoint phòng đang thuê tự xác định người dùng từ session, không nhận `userId`/`roomId` từ client.
- `GET /api/v1/admin/**`: chỉ role ADMIN.
- `POST /api/v1/tenant/dat-phong`, `GET /api/v1/tenant/dat-phong`: chỉ quản lý yêu cầu đặt phòng của người thuê đang đăng nhập.
- `/api/v1/rooms/{roomId}/media`: xem media theo chính sách truy cập hiện hành. Upload, sửa và xóa media là thao tác quản trị.

Gói PREMIUM là entitlement của người thuê, không phải role quản trị; PREMIUM không cấp quyền ADMIN.

## Media phòng

Media được lưu ngoài source frontend, trong thư mục cấu hình `ROOM_MEDIA_STORAGE_DIR` (mặc định `uploads`); mặc định các file phòng nằm dưới `uploads/rooms/`. Cần sao lưu thư mục này cùng database. Không đưa file upload hoặc thư mục uploads lên Git.
