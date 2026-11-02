-- Xác định server, instance và tài khoản Windows hiện tại trong SSMS.

SELECT
    @@SERVERNAME AS TenServer,
    SERVERPROPERTY('InstanceName') AS TenInstance,
    DB_NAME() AS DatabaseHienTai,
    SUSER_SNAME() AS TaiKhoanWindows;
GO

SELECT name, state_desc
FROM sys.databases
WHERE name = N'QuanLyPhongTro';
GO
