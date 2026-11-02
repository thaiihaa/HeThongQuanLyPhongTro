-- Chạy trong SSMS với Windows Authentication (quyền tạo database).
-- Không xóa database nếu đã tồn tại.

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'QuanLyPhongTro')
BEGIN
    CREATE DATABASE QuanLyPhongTro;
END
GO

SELECT name, state_desc
FROM sys.databases
WHERE name = N'QuanLyPhongTro';
GO
