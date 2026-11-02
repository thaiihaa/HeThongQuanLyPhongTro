SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF DB_NAME() <> N'QuanLyPhongTro'
    THROW 50003, 'Refusing to apply property schema outside QuanLyPhongTro.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.khu_tro', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.khu_tro
        (
            khu_tro_id BIGINT IDENTITY(1,1) NOT NULL,
            ma_khu_tro NVARCHAR(30) NOT NULL,
            ten_khu_tro NVARCHAR(150) NOT NULL,
            dia_chi NVARCHAR(500) NOT NULL,
            mo_ta NVARCHAR(1000) NULL,
            trang_thai NVARCHAR(20) NOT NULL CONSTRAINT DF_khu_tro_trang_thai DEFAULT N'HOAT_DONG',
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_khu_tro_created_at DEFAULT SYSUTCDATETIME(),
            updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_khu_tro_updated_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_khu_tro PRIMARY KEY (khu_tro_id),
            CONSTRAINT UQ_khu_tro_ma UNIQUE (ma_khu_tro),
            CONSTRAINT CK_khu_tro_trang_thai CHECK (trang_thai IN (N'HOAT_DONG', N'NGUNG_HOAT_DONG'))
        );
    END;

    IF OBJECT_ID(N'dbo.loai_phong', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.loai_phong
        (
            loai_phong_id INT IDENTITY(1,1) NOT NULL,
            ma_loai_phong NVARCHAR(30) NOT NULL,
            ten_loai_phong NVARCHAR(120) NOT NULL,
            dien_tich_m2 DECIMAL(8,2) NOT NULL,
            gia_thue_mac_dinh DECIMAL(19,2) NOT NULL,
            mo_ta NVARCHAR(1000) NULL,
            trang_thai NVARCHAR(20) NOT NULL CONSTRAINT DF_loai_phong_trang_thai DEFAULT N'HOAT_DONG',
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_loai_phong_created_at DEFAULT SYSUTCDATETIME(),
            updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_loai_phong_updated_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_loai_phong PRIMARY KEY (loai_phong_id),
            CONSTRAINT UQ_loai_phong_ma UNIQUE (ma_loai_phong),
            CONSTRAINT CK_loai_phong_dien_tich CHECK (dien_tich_m2 > 0),
            CONSTRAINT CK_loai_phong_gia CHECK (gia_thue_mac_dinh >= 0),
            CONSTRAINT CK_loai_phong_trang_thai CHECK (trang_thai IN (N'HOAT_DONG', N'NGUNG_HOAT_DONG'))
        );
    END;

    IF OBJECT_ID(N'dbo.phong_tro', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.phong_tro
        (
            phong_tro_id BIGINT IDENTITY(1,1) NOT NULL,
            khu_tro_id BIGINT NOT NULL,
            loai_phong_id INT NOT NULL,
            ma_phong NVARCHAR(30) NOT NULL,
            tang NVARCHAR(20) NULL,
            dien_tich_m2 DECIMAL(8,2) NOT NULL,
            gia_thue DECIMAL(19,2) NOT NULL,
            trang_thai NVARCHAR(24) NOT NULL CONSTRAINT DF_phong_tro_trang_thai DEFAULT N'TRONG',
            mo_ta NVARCHAR(1000) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_phong_tro_created_at DEFAULT SYSUTCDATETIME(),
            updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_phong_tro_updated_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_phong_tro PRIMARY KEY (phong_tro_id),
            CONSTRAINT UQ_phong_tro_khu_ma UNIQUE (khu_tro_id, ma_phong),
            CONSTRAINT FK_phong_tro_khu_tro FOREIGN KEY (khu_tro_id) REFERENCES dbo.khu_tro(khu_tro_id),
            CONSTRAINT FK_phong_tro_loai_phong FOREIGN KEY (loai_phong_id) REFERENCES dbo.loai_phong(loai_phong_id),
            CONSTRAINT CK_phong_tro_dien_tich CHECK (dien_tich_m2 > 0),
            CONSTRAINT CK_phong_tro_gia CHECK (gia_thue >= 0),
            CONSTRAINT CK_phong_tro_trang_thai CHECK (trang_thai IN (N'TRONG', N'DANG_THUE', N'DANG_SUA_CHUA', N'KHONG_KHA_DUNG'))
        );
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.phong_tro') AND name=N'IX_phong_tro_khu_trang_thai')
        CREATE INDEX IX_phong_tro_khu_trang_thai ON dbo.phong_tro(khu_tro_id, trang_thai);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.phong_tro') AND name=N'IX_phong_tro_loai_phong')
        CREATE INDEX IX_phong_tro_loai_phong ON dbo.phong_tro(loai_phong_id);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;