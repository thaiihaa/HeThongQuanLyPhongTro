SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF DB_NAME() <> N'QuanLyPhongTro'
    THROW 50001, 'Refusing to apply authentication schema outside QuanLyPhongTro.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.vai_tro', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.vai_tro
        (
            vai_tro_id INT IDENTITY(1,1) NOT NULL,
            ma_vai_tro NVARCHAR(40) NOT NULL,
            ten_vai_tro NVARCHAR(100) NOT NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_vai_tro_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_vai_tro PRIMARY KEY (vai_tro_id),
            CONSTRAINT UQ_vai_tro_ma_vai_tro UNIQUE (ma_vai_tro)
        );
    END;

    IF OBJECT_ID(N'dbo.nguoi_dung', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.nguoi_dung
        (
            nguoi_dung_id BIGINT IDENTITY(1,1) NOT NULL,
            vai_tro_id INT NOT NULL,
            ten_dang_nhap NVARCHAR(100) NOT NULL,
            email NVARCHAR(254) NULL,
            so_dien_thoai NVARCHAR(20) NULL,
            mat_khau_hash NVARCHAR(255) NOT NULL,
            ho_ten NVARCHAR(150) NOT NULL,
            trang_thai NVARCHAR(20) NOT NULL CONSTRAINT DF_nguoi_dung_trang_thai DEFAULT N'HOAT_DONG',
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_nguoi_dung_created_at DEFAULT SYSUTCDATETIME(),
            updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_nguoi_dung_updated_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_nguoi_dung PRIMARY KEY (nguoi_dung_id),
            CONSTRAINT UQ_nguoi_dung_ten_dang_nhap UNIQUE (ten_dang_nhap),
            CONSTRAINT FK_nguoi_dung_vai_tro FOREIGN KEY (vai_tro_id) REFERENCES dbo.vai_tro(vai_tro_id),
            CONSTRAINT CK_nguoi_dung_trang_thai CHECK (trang_thai IN (N'HOAT_DONG', N'KHOA'))
        );
    END;

    IF OBJECT_ID(N'dbo.nguoi_thue', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.nguoi_thue
        (
            nguoi_thue_id BIGINT IDENTITY(1,1) NOT NULL,
            nguoi_dung_id BIGINT NOT NULL,
            cccd NVARCHAR(20) NULL,
            ngay_sinh DATE NULL,
            dia_chi NVARCHAR(500) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_nguoi_thue_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_nguoi_thue PRIMARY KEY (nguoi_thue_id),
            CONSTRAINT UQ_nguoi_thue_nguoi_dung UNIQUE (nguoi_dung_id),
            CONSTRAINT FK_nguoi_thue_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id)
        );
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.nguoi_dung') AND name = N'UX_nguoi_dung_email')
        CREATE UNIQUE INDEX UX_nguoi_dung_email ON dbo.nguoi_dung(email) WHERE email IS NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.nguoi_dung') AND name = N'UX_nguoi_dung_so_dien_thoai')
        CREATE UNIQUE INDEX UX_nguoi_dung_so_dien_thoai ON dbo.nguoi_dung(so_dien_thoai) WHERE so_dien_thoai IS NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.nguoi_thue') AND name = N'UX_nguoi_thue_cccd')
        CREATE UNIQUE INDEX UX_nguoi_thue_cccd ON dbo.nguoi_thue(cccd) WHERE cccd IS NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM dbo.vai_tro WHERE ma_vai_tro = N'ADMIN')
        INSERT INTO dbo.vai_tro (ma_vai_tro, ten_vai_tro) VALUES (N'ADMIN', N'Quản trị viên');
    IF NOT EXISTS (SELECT 1 FROM dbo.vai_tro WHERE ma_vai_tro = N'QUAN_LY')
        INSERT INTO dbo.vai_tro (ma_vai_tro, ten_vai_tro) VALUES (N'QUAN_LY', N'Quản lý');
    IF NOT EXISTS (SELECT 1 FROM dbo.vai_tro WHERE ma_vai_tro = N'NHAN_VIEN')
        INSERT INTO dbo.vai_tro (ma_vai_tro, ten_vai_tro) VALUES (N'NHAN_VIEN', N'Nhân viên');
    IF NOT EXISTS (SELECT 1 FROM dbo.vai_tro WHERE ma_vai_tro = N'NGUOI_THUE')
        INSERT INTO dbo.vai_tro (ma_vai_tro, ten_vai_tro) VALUES (N'NGUOI_THUE', N'Người thuê');

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;