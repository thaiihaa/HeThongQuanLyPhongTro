-- Additive tenant plan catalog and entitlement history.
-- Does not modify existing tables or user/account data.
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF DB_NAME() <> N'QuanLyPhongTro'
    THROW 50020, 'Refusing to apply tenant plan schema outside QuanLyPhongTro.', 1;

IF OBJECT_ID(N'dbo.nguoi_thue', N'U') IS NULL
   OR OBJECT_ID(N'dbo.nguoi_dung', N'U') IS NULL
    THROW 50021, 'Required tenant and user tables are missing.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @ApplicationLockResult INT;
    EXEC @ApplicationLockResult = sys.sp_getapplock
        @Resource = N'QuanLyPhongTro:008_tenant_plans',
        @LockMode = N'Exclusive',
        @LockOwner = N'Transaction',
        @LockTimeout = 10000;
    IF @ApplicationLockResult < 0
        THROW 50022, 'Could not obtain the tenant plan schema lock.', 1;

    IF OBJECT_ID(N'dbo.goi_dich_vu', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.goi_dich_vu
        (
            ma_goi NVARCHAR(20) NOT NULL,
            ten_goi NVARCHAR(100) NOT NULL,
            mo_ta NVARCHAR(500) NOT NULL,
            created_at DATETIME2(0) NOT NULL
                CONSTRAINT DF_goi_dich_vu_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_goi_dich_vu PRIMARY KEY (ma_goi)
        );
    END;

    IF OBJECT_ID(N'dbo.nguoi_thue_goi', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.nguoi_thue_goi
        (
            nguoi_thue_goi_id BIGINT IDENTITY(1,1) NOT NULL,
            nguoi_thue_id BIGINT NOT NULL,
            ma_goi NVARCHAR(20) NOT NULL,
            ngay_bat_dau DATE NOT NULL,
            ngay_ket_thuc DATE NOT NULL,
            trang_thai NVARCHAR(20) NOT NULL
                CONSTRAINT DF_nguoi_thue_goi_trang_thai DEFAULT N'DANG_HOAT_DONG',
            nguoi_cap_id BIGINT NOT NULL,
            thu_hoi_at DATETIME2(0) NULL,
            nguoi_thu_hoi_id BIGINT NULL,
            created_at DATETIME2(0) NOT NULL
                CONSTRAINT DF_nguoi_thue_goi_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_nguoi_thue_goi PRIMARY KEY (nguoi_thue_goi_id),
            CONSTRAINT FK_nguoi_thue_goi_nguoi_thue
                FOREIGN KEY (nguoi_thue_id) REFERENCES dbo.nguoi_thue(nguoi_thue_id),
            CONSTRAINT FK_nguoi_thue_goi_catalog
                FOREIGN KEY (ma_goi) REFERENCES dbo.goi_dich_vu(ma_goi),
            CONSTRAINT FK_nguoi_thue_goi_nguoi_cap
                FOREIGN KEY (nguoi_cap_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id),
            CONSTRAINT FK_nguoi_thue_goi_nguoi_thu_hoi
                FOREIGN KEY (nguoi_thu_hoi_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id),
            CONSTRAINT CK_nguoi_thue_goi_ngay
                CHECK (ngay_ket_thuc >= ngay_bat_dau),
            CONSTRAINT CK_nguoi_thue_goi_trang_thai
                CHECK (
                    (trang_thai = N'DANG_HOAT_DONG' AND thu_hoi_at IS NULL AND nguoi_thu_hoi_id IS NULL)
                    OR
                    (trang_thai = N'DA_THU_HOI' AND thu_hoi_at IS NOT NULL AND nguoi_thu_hoi_id IS NOT NULL)
                )
        );
    END;

    IF NOT EXISTS (SELECT 1 FROM dbo.goi_dich_vu WHERE ma_goi = N'THUONG')
        INSERT INTO dbo.goi_dich_vu (ma_goi, ten_goi, mo_ta)
        VALUES (
            N'THUONG',
            N'Th' + NCHAR(432) + NCHAR(7901) + N'ng',
            N'Xem phong, gui yeu cau dat phong va theo doi yeu cau cua minh.'
        );

    IF NOT EXISTS (SELECT 1 FROM dbo.goi_dich_vu WHERE ma_goi = N'PREMIUM')
        INSERT INTO dbo.goi_dich_vu (ma_goi, ten_goi, mo_ta)
        VALUES (
            N'PREMIUM',
            N'Premium',
            N'Bao gom quyen loi goi Thuong va uu tien hien thi yeu cau dat phong trong hang doi Admin.'
        );

    IF NOT EXISTS (SELECT 1 FROM sys.indexes
                   WHERE object_id = OBJECT_ID(N'dbo.nguoi_thue_goi')
                     AND name = N'IX_nguoi_thue_goi_tenant_effective')
        CREATE INDEX IX_nguoi_thue_goi_tenant_effective
            ON dbo.nguoi_thue_goi(nguoi_thue_id, trang_thai, ngay_bat_dau, ngay_ket_thuc);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
