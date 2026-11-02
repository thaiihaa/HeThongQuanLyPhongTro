-- Additive business schema migration for QuanLyPhongTro.
-- Creates only missing target tables and indexes; never alters, drops, renames,
-- or seeds the existing tables or their data. Safe to rerun after a successful run.
-- Expected existing keys (verified in the current schema):
--   phong_tro.phong_tro_id BIGINT
--   nguoi_thue.nguoi_thue_id BIGINT
--   nguoi_dung.nguoi_dung_id BIGINT
-- This migration is additive and idempotent for tables created by this draft.

SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF DB_NAME() <> N'QuanLyPhongTro'
    THROW 50007, 'Refusing to apply business schema outside QuanLyPhongTro.', 1;

IF HAS_PERMS_BY_NAME(DB_NAME(), N'DATABASE', N'CREATE TABLE') <> 1
    THROW 50008, 'CREATE TABLE permission is required to apply this migration.', 1;

IF HAS_PERMS_BY_NAME(DB_NAME(), N'DATABASE', N'ALTER ANY SCHEMA') <> 1
    THROW 50009, 'ALTER ANY SCHEMA permission is required to apply this migration.', 1;

IF OBJECT_ID(N'dbo.khu_tro', N'U') IS NULL
   OR OBJECT_ID(N'dbo.loai_phong', N'U') IS NULL
   OR OBJECT_ID(N'dbo.phong_tro', N'U') IS NULL
   OR OBJECT_ID(N'dbo.nguoi_dung', N'U') IS NULL
   OR OBJECT_ID(N'dbo.nguoi_thue', N'U') IS NULL
    THROW 50010, 'Required existing property and account tables are missing.', 1;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.phong_tro') AND name = N'phong_tro_id' AND system_type_id = TYPE_ID(N'bigint'))
   OR NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.nguoi_thue') AND name = N'nguoi_thue_id' AND system_type_id = TYPE_ID(N'bigint'))
   OR NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.nguoi_dung') AND name = N'nguoi_dung_id' AND system_type_id = TYPE_ID(N'bigint'))
    THROW 50011, 'Existing referenced primary key columns do not match the verified BIGINT schema.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @ApplicationLockResult INT;
    EXEC @ApplicationLockResult = sys.sp_getapplock
        @Resource = N'QuanLyPhongTro:007_business_schema',
        @LockMode = N'Exclusive',
        @LockOwner = N'Transaction',
        @LockTimeout = 10000;
    IF @ApplicationLockResult < 0
        THROW 50012, 'Could not obtain the business-schema migration lock.', 1;

    IF OBJECT_ID(N'dbo.hop_dong_thue', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.hop_dong_thue
        (
            hop_dong_thue_id BIGINT IDENTITY(1,1) NOT NULL,
            phong_tro_id BIGINT NOT NULL,
            ma_hop_dong NVARCHAR(40) NOT NULL,
            ngay_bat_dau DATE NOT NULL,
            ngay_ket_thuc DATE NULL,
            gia_thue DECIMAL(19,2) NOT NULL,
            tien_coc DECIMAL(19,2) NOT NULL CONSTRAINT DF_hop_dong_tien_coc DEFAULT 0,
            ngay_thanh_toan_hang_thang TINYINT NOT NULL CONSTRAINT DF_hop_dong_ngay_thanh_toan DEFAULT 1,
            trang_thai NVARCHAR(24) NOT NULL CONSTRAINT DF_hop_dong_trang_thai DEFAULT N'CHO_DUYET',
            ghi_chu NVARCHAR(1000) NULL,
            created_by BIGINT NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_hop_dong_created_at DEFAULT SYSUTCDATETIME(),
            updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_hop_dong_updated_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_hop_dong_thue PRIMARY KEY (hop_dong_thue_id),
            CONSTRAINT UQ_hop_dong_thue_ma UNIQUE (ma_hop_dong),
            CONSTRAINT FK_hop_dong_thue_phong FOREIGN KEY (phong_tro_id) REFERENCES dbo.phong_tro(phong_tro_id),
            CONSTRAINT FK_hop_dong_thue_nguoi_tao FOREIGN KEY (created_by) REFERENCES dbo.nguoi_dung(nguoi_dung_id),
            CONSTRAINT CK_hop_dong_thue_ngay CHECK (ngay_ket_thuc IS NULL OR ngay_ket_thuc >= ngay_bat_dau),
            CONSTRAINT CK_hop_dong_thue_gia CHECK (gia_thue >= 0 AND tien_coc >= 0),
            CONSTRAINT CK_hop_dong_thue_ngay_tt CHECK (ngay_thanh_toan_hang_thang BETWEEN 1 AND 31),
            CONSTRAINT CK_hop_dong_thue_trang_thai CHECK (trang_thai IN (N'CHO_DUYET', N'HIEU_LUC', N'HET_HAN', N'THANH_LY', N'TU_CHOI'))
        );
    END;

    IF OBJECT_ID(N'dbo.hop_dong_nguoi_thue', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.hop_dong_nguoi_thue
        (
            hop_dong_nguoi_thue_id BIGINT IDENTITY(1,1) NOT NULL,
            hop_dong_thue_id BIGINT NOT NULL,
            nguoi_thue_id BIGINT NOT NULL,
            la_dai_dien BIT NOT NULL CONSTRAINT DF_hop_dong_nguoi_thue_dai_dien DEFAULT 0,
            ngay_tham_gia DATE NOT NULL CONSTRAINT DF_hop_dong_nguoi_thue_ngay DEFAULT CONVERT(DATE, SYSUTCDATETIME()),
            ngay_roi_di DATE NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_hop_dong_nguoi_thue_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_hop_dong_nguoi_thue PRIMARY KEY (hop_dong_nguoi_thue_id),
            CONSTRAINT UQ_hop_dong_nguoi_thue UNIQUE (hop_dong_thue_id, nguoi_thue_id),
            CONSTRAINT FK_hop_dong_nguoi_thue_hop_dong FOREIGN KEY (hop_dong_thue_id) REFERENCES dbo.hop_dong_thue(hop_dong_thue_id),
            CONSTRAINT FK_hop_dong_nguoi_thue_nguoi_thue FOREIGN KEY (nguoi_thue_id) REFERENCES dbo.nguoi_thue(nguoi_thue_id),
            CONSTRAINT CK_hop_dong_nguoi_thue_ngay CHECK (ngay_roi_di IS NULL OR ngay_roi_di >= ngay_tham_gia)
        );
    END;

    IF OBJECT_ID(N'dbo.lich_su_dieu_chinh_hop_dong', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.lich_su_dieu_chinh_hop_dong
        (
            lich_su_dieu_chinh_hop_dong_id BIGINT IDENTITY(1,1) NOT NULL,
            hop_dong_thue_id BIGINT NOT NULL,
            nguoi_dung_id BIGINT NULL,
            loai_dieu_chinh NVARCHAR(60) NOT NULL,
            gia_tri_cu NVARCHAR(1000) NULL,
            gia_tri_moi NVARCHAR(1000) NULL,
            ly_do NVARCHAR(1000) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_lich_su_hop_dong_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_lich_su_dieu_chinh_hop_dong PRIMARY KEY (lich_su_dieu_chinh_hop_dong_id),
            CONSTRAINT FK_lich_su_hop_dong_hop_dong FOREIGN KEY (hop_dong_thue_id) REFERENCES dbo.hop_dong_thue(hop_dong_thue_id),
            CONSTRAINT FK_lich_su_hop_dong_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id)
        );
    END;

    IF OBJECT_ID(N'dbo.dich_vu', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.dich_vu
        (
            dich_vu_id BIGINT IDENTITY(1,1) NOT NULL,
            ma_dich_vu NVARCHAR(40) NOT NULL,
            ten_dich_vu NVARCHAR(150) NOT NULL,
            don_vi_tinh NVARCHAR(40) NOT NULL,
            don_gia DECIMAL(19,2) NOT NULL,
            cach_tinh NVARCHAR(24) NOT NULL CONSTRAINT DF_dich_vu_cach_tinh DEFAULT N'CO_DINH',
            trang_thai NVARCHAR(20) NOT NULL CONSTRAINT DF_dich_vu_trang_thai DEFAULT N'HOAT_DONG',
            mo_ta NVARCHAR(1000) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_dich_vu_created_at DEFAULT SYSUTCDATETIME(),
            updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_dich_vu_updated_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_dich_vu PRIMARY KEY (dich_vu_id),
            CONSTRAINT UQ_dich_vu_ma UNIQUE (ma_dich_vu),
            CONSTRAINT CK_dich_vu_gia CHECK (don_gia >= 0),
            CONSTRAINT CK_dich_vu_cach_tinh CHECK (cach_tinh IN (N'CO_DINH', N'THEO_SO_LUONG', N'THEO_CHI_SO')),
            CONSTRAINT CK_dich_vu_trang_thai CHECK (trang_thai IN (N'HOAT_DONG', N'NGUNG_HOAT_DONG'))
        );
    END;

    IF OBJECT_ID(N'dbo.dich_vu_hop_dong', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.dich_vu_hop_dong
        (
            dich_vu_hop_dong_id BIGINT IDENTITY(1,1) NOT NULL,
            hop_dong_thue_id BIGINT NOT NULL,
            dich_vu_id BIGINT NOT NULL,
            don_gia_ap_dung DECIMAL(19,2) NOT NULL,
            ngay_bat_dau DATE NOT NULL,
            ngay_ket_thuc DATE NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_dich_vu_hop_dong_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_dich_vu_hop_dong PRIMARY KEY (dich_vu_hop_dong_id),
            CONSTRAINT UQ_dich_vu_hop_dong UNIQUE (hop_dong_thue_id, dich_vu_id, ngay_bat_dau),
            CONSTRAINT FK_dich_vu_hop_dong_hop_dong FOREIGN KEY (hop_dong_thue_id) REFERENCES dbo.hop_dong_thue(hop_dong_thue_id),
            CONSTRAINT FK_dich_vu_hop_dong_dich_vu FOREIGN KEY (dich_vu_id) REFERENCES dbo.dich_vu(dich_vu_id),
            CONSTRAINT CK_dich_vu_hop_dong_gia CHECK (don_gia_ap_dung >= 0),
            CONSTRAINT CK_dich_vu_hop_dong_ngay CHECK (ngay_ket_thuc IS NULL OR ngay_ket_thuc >= ngay_bat_dau)
        );
    END;

    IF OBJECT_ID(N'dbo.danh_muc_tien_nghi', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.danh_muc_tien_nghi
        (
            danh_muc_tien_nghi_id BIGINT IDENTITY(1,1) NOT NULL,
            ma_tien_nghi NVARCHAR(40) NOT NULL,
            ten_tien_nghi NVARCHAR(150) NOT NULL,
            mo_ta NVARCHAR(1000) NULL,
            trang_thai NVARCHAR(20) NOT NULL CONSTRAINT DF_danh_muc_tien_nghi_trang_thai DEFAULT N'HOAT_DONG',
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_danh_muc_tien_nghi_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_danh_muc_tien_nghi PRIMARY KEY (danh_muc_tien_nghi_id),
            CONSTRAINT UQ_danh_muc_tien_nghi_ma UNIQUE (ma_tien_nghi),
            CONSTRAINT CK_danh_muc_tien_nghi_trang_thai CHECK (trang_thai IN (N'HOAT_DONG', N'NGUNG_HOAT_DONG'))
        );
    END;

    IF OBJECT_ID(N'dbo.tien_nghi_phong', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.tien_nghi_phong
        (
            tien_nghi_phong_id BIGINT IDENTITY(1,1) NOT NULL,
            phong_tro_id BIGINT NOT NULL,
            danh_muc_tien_nghi_id BIGINT NOT NULL,
            so_luong INT NOT NULL CONSTRAINT DF_tien_nghi_phong_so_luong DEFAULT 1,
            tinh_trang NVARCHAR(40) NULL,
            ghi_chu NVARCHAR(500) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_tien_nghi_phong_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_tien_nghi_phong PRIMARY KEY (tien_nghi_phong_id),
            CONSTRAINT UQ_tien_nghi_phong UNIQUE (phong_tro_id, danh_muc_tien_nghi_id),
            CONSTRAINT FK_tien_nghi_phong_phong FOREIGN KEY (phong_tro_id) REFERENCES dbo.phong_tro(phong_tro_id),
            CONSTRAINT FK_tien_nghi_phong_danh_muc FOREIGN KEY (danh_muc_tien_nghi_id) REFERENCES dbo.danh_muc_tien_nghi(danh_muc_tien_nghi_id),
            CONSTRAINT CK_tien_nghi_phong_so_luong CHECK (so_luong > 0)
        );
    END;

    IF OBJECT_ID(N'dbo.cong_to', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.cong_to
        (
            cong_to_id BIGINT IDENTITY(1,1) NOT NULL,
            phong_tro_id BIGINT NOT NULL,
            dich_vu_id BIGINT NOT NULL,
            ma_cong_to NVARCHAR(60) NOT NULL,
            ngay_lap DATE NOT NULL,
            ngay_thao DATE NULL,
            chi_so_dau DECIMAL(19,3) NOT NULL CONSTRAINT DF_cong_to_chi_so_dau DEFAULT 0,
            trang_thai NVARCHAR(20) NOT NULL CONSTRAINT DF_cong_to_trang_thai DEFAULT N'DANG_SU_DUNG',
            ghi_chu NVARCHAR(500) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_cong_to_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_cong_to PRIMARY KEY (cong_to_id),
            CONSTRAINT UQ_cong_to_ma UNIQUE (ma_cong_to),
            CONSTRAINT FK_cong_to_phong FOREIGN KEY (phong_tro_id) REFERENCES dbo.phong_tro(phong_tro_id),
            CONSTRAINT FK_cong_to_dich_vu FOREIGN KEY (dich_vu_id) REFERENCES dbo.dich_vu(dich_vu_id),
            CONSTRAINT CK_cong_to_chi_so CHECK (chi_so_dau >= 0),
            CONSTRAINT CK_cong_to_ngay CHECK (ngay_thao IS NULL OR ngay_thao >= ngay_lap),
            CONSTRAINT CK_cong_to_trang_thai CHECK (trang_thai IN (N'DANG_SU_DUNG', N'NGUNG_SU_DUNG'))
        );
    END;

    IF OBJECT_ID(N'dbo.chi_so_cong_to', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.chi_so_cong_to
        (
            chi_so_cong_to_id BIGINT IDENTITY(1,1) NOT NULL,
            cong_to_id BIGINT NOT NULL,
            ky_ghi DATE NOT NULL,
            chi_so DECIMAL(19,3) NOT NULL,
            anh_chi_so_url NVARCHAR(1000) NULL,
            nguoi_ghi_id BIGINT NULL,
            ghi_chu NVARCHAR(500) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_chi_so_cong_to_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_chi_so_cong_to PRIMARY KEY (chi_so_cong_to_id),
            CONSTRAINT UQ_chi_so_cong_to_ky UNIQUE (cong_to_id, ky_ghi),
            CONSTRAINT FK_chi_so_cong_to_cong_to FOREIGN KEY (cong_to_id) REFERENCES dbo.cong_to(cong_to_id),
            CONSTRAINT FK_chi_so_cong_to_nguoi_ghi FOREIGN KEY (nguoi_ghi_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id),
            CONSTRAINT CK_chi_so_cong_to_chi_so CHECK (chi_so >= 0)
        );
    END;

    IF OBJECT_ID(N'dbo.hoa_don', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.hoa_don
        (
            hoa_don_id BIGINT IDENTITY(1,1) NOT NULL,
            hop_dong_thue_id BIGINT NOT NULL,
            ky_hoa_don DATE NOT NULL,
            ngay_phat_hanh DATE NOT NULL CONSTRAINT DF_hoa_don_ngay_phat_hanh DEFAULT CONVERT(DATE, SYSUTCDATETIME()),
            han_thanh_toan DATE NOT NULL,
            tong_tien DECIMAL(19,2) NOT NULL CONSTRAINT DF_hoa_don_tong_tien DEFAULT 0,
            trang_thai NVARCHAR(24) NOT NULL CONSTRAINT DF_hoa_don_trang_thai DEFAULT N'CHUA_THANH_TOAN',
            ghi_chu NVARCHAR(1000) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_hoa_don_created_at DEFAULT SYSUTCDATETIME(),
            updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_hoa_don_updated_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_hoa_don PRIMARY KEY (hoa_don_id),
            CONSTRAINT UQ_hoa_don_hop_dong_ky UNIQUE (hop_dong_thue_id, ky_hoa_don),
            CONSTRAINT FK_hoa_don_hop_dong FOREIGN KEY (hop_dong_thue_id) REFERENCES dbo.hop_dong_thue(hop_dong_thue_id),
            CONSTRAINT CK_hoa_don_tien CHECK (tong_tien >= 0),
            CONSTRAINT CK_hoa_don_trang_thai CHECK (trang_thai IN (N'CHUA_THANH_TOAN', N'THANH_TOAN_MOT_PHAN', N'DA_THANH_TOAN', N'QUA_HAN', N'HUY'))
        );
    END;

    IF OBJECT_ID(N'dbo.chi_tiet_hoa_don', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.chi_tiet_hoa_don
        (
            chi_tiet_hoa_don_id BIGINT IDENTITY(1,1) NOT NULL,
            hoa_don_id BIGINT NOT NULL,
            dich_vu_hop_dong_id BIGINT NULL,
            chi_so_cong_to_id BIGINT NULL,
            ten_khoan_thu NVARCHAR(200) NOT NULL,
            don_vi_tinh NVARCHAR(40) NULL,
            so_luong DECIMAL(19,3) NOT NULL CONSTRAINT DF_chi_tiet_hoa_don_so_luong DEFAULT 1,
            don_gia DECIMAL(19,2) NOT NULL,
            thanh_tien DECIMAL(19,2) NOT NULL,
            ghi_chu NVARCHAR(500) NULL,
            CONSTRAINT PK_chi_tiet_hoa_don PRIMARY KEY (chi_tiet_hoa_don_id),
            CONSTRAINT FK_chi_tiet_hoa_don_hoa_don FOREIGN KEY (hoa_don_id) REFERENCES dbo.hoa_don(hoa_don_id),
            CONSTRAINT FK_chi_tiet_hoa_don_dich_vu FOREIGN KEY (dich_vu_hop_dong_id) REFERENCES dbo.dich_vu_hop_dong(dich_vu_hop_dong_id),
            CONSTRAINT FK_chi_tiet_hoa_don_chi_so FOREIGN KEY (chi_so_cong_to_id) REFERENCES dbo.chi_so_cong_to(chi_so_cong_to_id),
            CONSTRAINT CK_chi_tiet_hoa_don_tien CHECK (so_luong >= 0 AND don_gia >= 0 AND thanh_tien >= 0)
        );
    END;

    IF OBJECT_ID(N'dbo.dieu_chinh_hoa_don', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.dieu_chinh_hoa_don
        (
            dieu_chinh_hoa_don_id BIGINT IDENTITY(1,1) NOT NULL,
            hoa_don_id BIGINT NOT NULL,
            nguoi_dung_id BIGINT NULL,
            so_tien DECIMAL(19,2) NOT NULL,
            ly_do NVARCHAR(1000) NOT NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_dieu_chinh_hoa_don_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_dieu_chinh_hoa_don PRIMARY KEY (dieu_chinh_hoa_don_id),
            CONSTRAINT FK_dieu_chinh_hoa_don_hoa_don FOREIGN KEY (hoa_don_id) REFERENCES dbo.hoa_don(hoa_don_id),
            CONSTRAINT FK_dieu_chinh_hoa_don_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id)
        );
    END;

    IF OBJECT_ID(N'dbo.thanh_toan', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.thanh_toan
        (
            thanh_toan_id BIGINT IDENTITY(1,1) NOT NULL,
            nguoi_thue_id BIGINT NOT NULL,
            ma_giao_dich NVARCHAR(80) NOT NULL,
            so_tien DECIMAL(19,2) NOT NULL,
            phuong_thuc NVARCHAR(40) NOT NULL,
            trang_thai NVARCHAR(24) NOT NULL CONSTRAINT DF_thanh_toan_trang_thai DEFAULT N'CHO_XAC_NHAN',
            thoi_diem_thanh_toan DATETIME2(0) NULL,
            ghi_chu NVARCHAR(1000) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_thanh_toan_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_thanh_toan PRIMARY KEY (thanh_toan_id),
            CONSTRAINT UQ_thanh_toan_ma_giao_dich UNIQUE (ma_giao_dich),
            CONSTRAINT FK_thanh_toan_nguoi_thue FOREIGN KEY (nguoi_thue_id) REFERENCES dbo.nguoi_thue(nguoi_thue_id),
            CONSTRAINT CK_thanh_toan_so_tien CHECK (so_tien > 0),
            CONSTRAINT CK_thanh_toan_trang_thai CHECK (trang_thai IN (N'CHO_XAC_NHAN', N'DA_XAC_NHAN', N'TU_CHOI', N'HOAN_TIEN'))
        );
    END;

    IF OBJECT_ID(N'dbo.phan_bo_thanh_toan', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.phan_bo_thanh_toan
        (
            phan_bo_thanh_toan_id BIGINT IDENTITY(1,1) NOT NULL,
            thanh_toan_id BIGINT NOT NULL,
            hoa_don_id BIGINT NOT NULL,
            so_tien_phan_bo DECIMAL(19,2) NOT NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_phan_bo_thanh_toan_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_phan_bo_thanh_toan PRIMARY KEY (phan_bo_thanh_toan_id),
            CONSTRAINT UQ_phan_bo_thanh_toan UNIQUE (thanh_toan_id, hoa_don_id),
            CONSTRAINT FK_phan_bo_thanh_toan_thanh_toan FOREIGN KEY (thanh_toan_id) REFERENCES dbo.thanh_toan(thanh_toan_id),
            CONSTRAINT FK_phan_bo_thanh_toan_hoa_don FOREIGN KEY (hoa_don_id) REFERENCES dbo.hoa_don(hoa_don_id),
            CONSTRAINT CK_phan_bo_thanh_toan_so_tien CHECK (so_tien_phan_bo > 0)
        );
    END;

    IF OBJECT_ID(N'dbo.nha_cung_cap', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.nha_cung_cap
        (
            nha_cung_cap_id BIGINT IDENTITY(1,1) NOT NULL,
            ma_nha_cung_cap NVARCHAR(40) NOT NULL,
            ten_nha_cung_cap NVARCHAR(200) NOT NULL,
            nguoi_lien_he NVARCHAR(150) NULL,
            so_dien_thoai NVARCHAR(20) NULL,
            email NVARCHAR(254) NULL,
            dia_chi NVARCHAR(500) NULL,
            ma_so_thue NVARCHAR(40) NULL,
            trang_thai NVARCHAR(20) NOT NULL CONSTRAINT DF_nha_cung_cap_trang_thai DEFAULT N'HOAT_DONG',
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_nha_cung_cap_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_nha_cung_cap PRIMARY KEY (nha_cung_cap_id),
            CONSTRAINT UQ_nha_cung_cap_ma UNIQUE (ma_nha_cung_cap),
            CONSTRAINT CK_nha_cung_cap_trang_thai CHECK (trang_thai IN (N'HOAT_DONG', N'NGUNG_HOAT_DONG'))
        );
    END;

    IF OBJECT_ID(N'dbo.chi_phi', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.chi_phi
        (
            chi_phi_id BIGINT IDENTITY(1,1) NOT NULL,
            nha_cung_cap_id BIGINT NULL,
            phong_tro_id BIGINT NULL,
            nguoi_tao_id BIGINT NULL,
            ma_chi_phi NVARCHAR(40) NOT NULL,
            loai_chi_phi NVARCHAR(80) NOT NULL,
            noi_dung NVARCHAR(500) NOT NULL,
            so_tien DECIMAL(19,2) NOT NULL,
            ngay_chi DATE NOT NULL,
            trang_thai NVARCHAR(24) NOT NULL CONSTRAINT DF_chi_phi_trang_thai DEFAULT N'CHO_DUYET',
            chung_tu_url NVARCHAR(1000) NULL,
            ghi_chu NVARCHAR(1000) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_chi_phi_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_chi_phi PRIMARY KEY (chi_phi_id),
            CONSTRAINT UQ_chi_phi_ma UNIQUE (ma_chi_phi),
            CONSTRAINT FK_chi_phi_nha_cung_cap FOREIGN KEY (nha_cung_cap_id) REFERENCES dbo.nha_cung_cap(nha_cung_cap_id),
            CONSTRAINT FK_chi_phi_phong FOREIGN KEY (phong_tro_id) REFERENCES dbo.phong_tro(phong_tro_id),
            CONSTRAINT FK_chi_phi_nguoi_tao FOREIGN KEY (nguoi_tao_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id),
            CONSTRAINT CK_chi_phi_so_tien CHECK (so_tien >= 0),
            CONSTRAINT CK_chi_phi_trang_thai CHECK (trang_thai IN (N'CHO_DUYET', N'DA_DUYET', N'TU_CHOI'))
        );
    END;

    IF OBJECT_ID(N'dbo.yeu_cau_sua_chua', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.yeu_cau_sua_chua
        (
            yeu_cau_sua_chua_id BIGINT IDENTITY(1,1) NOT NULL,
            phong_tro_id BIGINT NOT NULL,
            nguoi_thue_id BIGINT NULL,
            nguoi_phu_trach_id BIGINT NULL,
            ma_yeu_cau NVARCHAR(40) NOT NULL,
            tieu_de NVARCHAR(200) NOT NULL,
            mo_ta NVARCHAR(2000) NOT NULL,
            muc_do_uu_tien NVARCHAR(20) NOT NULL CONSTRAINT DF_yeu_cau_sua_chua_uu_tien DEFAULT N'THUONG',
            trang_thai NVARCHAR(24) NOT NULL CONSTRAINT DF_yeu_cau_sua_chua_trang_thai DEFAULT N'MOI',
            ngay_tiep_nhan DATETIME2(0) NOT NULL CONSTRAINT DF_yeu_cau_sua_chua_ngay DEFAULT SYSUTCDATETIME(),
            ngay_hoan_thanh DATETIME2(0) NULL,
            chi_phi_du_kien DECIMAL(19,2) NULL,
            ghi_chu NVARCHAR(1000) NULL,
            CONSTRAINT PK_yeu_cau_sua_chua PRIMARY KEY (yeu_cau_sua_chua_id),
            CONSTRAINT UQ_yeu_cau_sua_chua_ma UNIQUE (ma_yeu_cau),
            CONSTRAINT FK_yeu_cau_sua_chua_phong FOREIGN KEY (phong_tro_id) REFERENCES dbo.phong_tro(phong_tro_id),
            CONSTRAINT FK_yeu_cau_sua_chua_nguoi_thue FOREIGN KEY (nguoi_thue_id) REFERENCES dbo.nguoi_thue(nguoi_thue_id),
            CONSTRAINT FK_yeu_cau_sua_chua_nguoi_phu_trach FOREIGN KEY (nguoi_phu_trach_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id),
            CONSTRAINT CK_yeu_cau_sua_chua_chi_phi CHECK (chi_phi_du_kien IS NULL OR chi_phi_du_kien >= 0),
            CONSTRAINT CK_yeu_cau_sua_chua_uu_tien CHECK (muc_do_uu_tien IN (N'THAP', N'THUONG', N'CAO', N'KHAN_CAP')),
            CONSTRAINT CK_yeu_cau_sua_chua_trang_thai CHECK (trang_thai IN (N'MOI', N'DA_TIEP_NHAN', N'DANG_XU_LY', N'HOAN_TAT', N'HUY'))
        );
    END;

    IF OBJECT_ID(N'dbo.vat_tu_sua_chua', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.vat_tu_sua_chua
        (
            vat_tu_sua_chua_id BIGINT IDENTITY(1,1) NOT NULL,
            yeu_cau_sua_chua_id BIGINT NOT NULL,
            nha_cung_cap_id BIGINT NULL,
            ten_vat_tu NVARCHAR(200) NOT NULL,
            don_vi_tinh NVARCHAR(40) NULL,
            so_luong DECIMAL(19,3) NOT NULL,
            don_gia DECIMAL(19,2) NOT NULL,
            ghi_chu NVARCHAR(500) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_vat_tu_sua_chua_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_vat_tu_sua_chua PRIMARY KEY (vat_tu_sua_chua_id),
            CONSTRAINT FK_vat_tu_sua_chua_yeu_cau FOREIGN KEY (yeu_cau_sua_chua_id) REFERENCES dbo.yeu_cau_sua_chua(yeu_cau_sua_chua_id),
            CONSTRAINT FK_vat_tu_sua_chua_nha_cung_cap FOREIGN KEY (nha_cung_cap_id) REFERENCES dbo.nha_cung_cap(nha_cung_cap_id),
            CONSTRAINT CK_vat_tu_sua_chua_chi_phi CHECK (so_luong > 0 AND don_gia >= 0)
        );
    END;

    IF OBJECT_ID(N'dbo.tai_san_phong', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.tai_san_phong
        (
            tai_san_phong_id BIGINT IDENTITY(1,1) NOT NULL,
            phong_tro_id BIGINT NOT NULL,
            ma_tai_san NVARCHAR(40) NOT NULL,
            ten_tai_san NVARCHAR(200) NOT NULL,
            so_luong INT NOT NULL CONSTRAINT DF_tai_san_phong_so_luong DEFAULT 1,
            ngay_mua DATE NULL,
            nguyen_gia DECIMAL(19,2) NULL,
            tinh_trang NVARCHAR(40) NOT NULL CONSTRAINT DF_tai_san_phong_tinh_trang DEFAULT N'TOT',
            ghi_chu NVARCHAR(1000) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_tai_san_phong_created_at DEFAULT SYSUTCDATETIME(),
            updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_tai_san_phong_updated_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_tai_san_phong PRIMARY KEY (tai_san_phong_id),
            CONSTRAINT UQ_tai_san_phong_ma UNIQUE (ma_tai_san),
            CONSTRAINT FK_tai_san_phong_phong FOREIGN KEY (phong_tro_id) REFERENCES dbo.phong_tro(phong_tro_id),
            CONSTRAINT CK_tai_san_phong_gia CHECK (so_luong > 0 AND (nguyen_gia IS NULL OR nguyen_gia >= 0))
        );
    END;

    IF OBJECT_ID(N'dbo.bien_ban_ban_giao_tai_san', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.bien_ban_ban_giao_tai_san
        (
            bien_ban_ban_giao_tai_san_id BIGINT IDENTITY(1,1) NOT NULL,
            hop_dong_thue_id BIGINT NOT NULL,
            tai_san_phong_id BIGINT NOT NULL,
            nguoi_thue_id BIGINT NOT NULL,
            so_luong INT NOT NULL,
            tinh_trang_ban_giao NVARCHAR(500) NULL,
            tinh_trang_tra NVARCHAR(500) NULL,
            ngay_ban_giao DATE NOT NULL,
            ngay_tra DATE NULL,
            ghi_chu NVARCHAR(1000) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_bien_ban_ban_giao_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_bien_ban_ban_giao_tai_san PRIMARY KEY (bien_ban_ban_giao_tai_san_id),
            CONSTRAINT FK_bien_ban_ban_giao_hop_dong FOREIGN KEY (hop_dong_thue_id) REFERENCES dbo.hop_dong_thue(hop_dong_thue_id),
            CONSTRAINT FK_bien_ban_ban_giao_tai_san FOREIGN KEY (tai_san_phong_id) REFERENCES dbo.tai_san_phong(tai_san_phong_id),
            CONSTRAINT FK_bien_ban_ban_giao_nguoi_thue FOREIGN KEY (nguoi_thue_id) REFERENCES dbo.nguoi_thue(nguoi_thue_id),
            CONSTRAINT CK_bien_ban_ban_giao_so_luong CHECK (so_luong > 0),
            CONSTRAINT CK_bien_ban_ban_giao_ngay CHECK (ngay_tra IS NULL OR ngay_tra >= ngay_ban_giao)
        );
    END;

    IF OBJECT_ID(N'dbo.kiem_tra_phong', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.kiem_tra_phong
        (
            kiem_tra_phong_id BIGINT IDENTITY(1,1) NOT NULL,
            phong_tro_id BIGINT NOT NULL,
            hop_dong_thue_id BIGINT NULL,
            nguoi_kiem_tra_id BIGINT NULL,
            loai_kiem_tra NVARCHAR(40) NOT NULL,
            ngay_kiem_tra DATETIME2(0) NOT NULL,
            ket_luan NVARCHAR(60) NULL,
            ghi_chu NVARCHAR(2000) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_kiem_tra_phong_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_kiem_tra_phong PRIMARY KEY (kiem_tra_phong_id),
            CONSTRAINT FK_kiem_tra_phong_phong FOREIGN KEY (phong_tro_id) REFERENCES dbo.phong_tro(phong_tro_id),
            CONSTRAINT FK_kiem_tra_phong_hop_dong FOREIGN KEY (hop_dong_thue_id) REFERENCES dbo.hop_dong_thue(hop_dong_thue_id),
            CONSTRAINT FK_kiem_tra_phong_nguoi_kiem_tra FOREIGN KEY (nguoi_kiem_tra_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id)
        );
    END;

    IF OBJECT_ID(N'dbo.chi_tiet_kiem_tra_phong', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.chi_tiet_kiem_tra_phong
        (
            chi_tiet_kiem_tra_phong_id BIGINT IDENTITY(1,1) NOT NULL,
            kiem_tra_phong_id BIGINT NOT NULL,
            hang_muc NVARCHAR(200) NOT NULL,
            ket_qua NVARCHAR(40) NOT NULL,
            mo_ta NVARCHAR(1000) NULL,
            anh_url NVARCHAR(1000) NULL,
            thu_tu INT NOT NULL CONSTRAINT DF_chi_tiet_kiem_tra_thu_tu DEFAULT 0,
            CONSTRAINT PK_chi_tiet_kiem_tra_phong PRIMARY KEY (chi_tiet_kiem_tra_phong_id),
            CONSTRAINT FK_chi_tiet_kiem_tra_phong_kiem_tra FOREIGN KEY (kiem_tra_phong_id) REFERENCES dbo.kiem_tra_phong(kiem_tra_phong_id),
            CONSTRAINT CK_chi_tiet_kiem_tra_thu_tu CHECK (thu_tu >= 0)
        );
    END;

    IF OBJECT_ID(N'dbo.thong_bao', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.thong_bao
        (
            thong_bao_id BIGINT IDENTITY(1,1) NOT NULL,
            nguoi_dung_id BIGINT NOT NULL,
            tieu_de NVARCHAR(200) NOT NULL,
            noi_dung NVARCHAR(2000) NOT NULL,
            loai_thong_bao NVARCHAR(40) NOT NULL,
            lien_ket NVARCHAR(500) NULL,
            da_doc BIT NOT NULL CONSTRAINT DF_thong_bao_da_doc DEFAULT 0,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_thong_bao_created_at DEFAULT SYSUTCDATETIME(),
            read_at DATETIME2(0) NULL,
            CONSTRAINT PK_thong_bao PRIMARY KEY (thong_bao_id),
            CONSTRAINT FK_thong_bao_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id)
        );
    END;

    IF OBJECT_ID(N'dbo.nhat_ky_he_thong', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.nhat_ky_he_thong
        (
            nhat_ky_he_thong_id BIGINT IDENTITY(1,1) NOT NULL,
            nguoi_dung_id BIGINT NULL,
            hanh_dong NVARCHAR(100) NOT NULL,
            doi_tuong NVARCHAR(100) NOT NULL,
            doi_tuong_id NVARCHAR(100) NULL,
            du_lieu_cu NVARCHAR(MAX) NULL,
            du_lieu_moi NVARCHAR(MAX) NULL,
            dia_chi_ip NVARCHAR(45) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_nhat_ky_he_thong_created_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_nhat_ky_he_thong PRIMARY KEY (nhat_ky_he_thong_id),
            CONSTRAINT FK_nhat_ky_he_thong_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id)
        );
    END;

    IF OBJECT_ID(N'dbo.dat_phong', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.dat_phong
        (
            dat_phong_id BIGINT IDENTITY(1,1) NOT NULL,
            phong_tro_id BIGINT NOT NULL,
            nguoi_dung_id BIGINT NULL,
            ho_ten_nguoi_dat NVARCHAR(150) NOT NULL,
            so_dien_thoai NVARCHAR(20) NOT NULL,
            email NVARCHAR(254) NULL,
            ngay_nhan_phong_du_kien DATE NOT NULL,
            ngay_tra_phong_du_kien DATE NULL,
            tien_giu_cho DECIMAL(19,2) NOT NULL CONSTRAINT DF_dat_phong_tien_giu_cho DEFAULT 0,
            trang_thai NVARCHAR(24) NOT NULL CONSTRAINT DF_dat_phong_trang_thai DEFAULT N'CHO_XAC_NHAN',
            ghi_chu NVARCHAR(1000) NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_dat_phong_created_at DEFAULT SYSUTCDATETIME(),
            updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_dat_phong_updated_at DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_dat_phong PRIMARY KEY (dat_phong_id),
            CONSTRAINT FK_dat_phong_phong FOREIGN KEY (phong_tro_id) REFERENCES dbo.phong_tro(phong_tro_id),
            CONSTRAINT FK_dat_phong_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id),
            CONSTRAINT CK_dat_phong_ngay CHECK (ngay_tra_phong_du_kien IS NULL OR ngay_tra_phong_du_kien >= ngay_nhan_phong_du_kien),
            CONSTRAINT CK_dat_phong_tien CHECK (tien_giu_cho >= 0),
            CONSTRAINT CK_dat_phong_trang_thai CHECK (trang_thai IN (N'CHO_XAC_NHAN', N'DA_XAC_NHAN', N'DA_HUY', N'HOAN_TAT'))
        );
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.hop_dong_thue') AND name = N'IX_hop_dong_thue_phong_trang_thai')
        CREATE INDEX IX_hop_dong_thue_phong_trang_thai ON dbo.hop_dong_thue(phong_tro_id, trang_thai);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.hop_dong_nguoi_thue') AND name = N'IX_hop_dong_nguoi_thue_nguoi_thue')
        CREATE INDEX IX_hop_dong_nguoi_thue_nguoi_thue ON dbo.hop_dong_nguoi_thue(nguoi_thue_id, hop_dong_thue_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.dich_vu_hop_dong') AND name = N'IX_dich_vu_hop_dong_hop_dong')
        CREATE INDEX IX_dich_vu_hop_dong_hop_dong ON dbo.dich_vu_hop_dong(hop_dong_thue_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.cong_to') AND name = N'IX_cong_to_phong_dich_vu')
        CREATE INDEX IX_cong_to_phong_dich_vu ON dbo.cong_to(phong_tro_id, dich_vu_id, trang_thai);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chi_so_cong_to') AND name = N'IX_chi_so_cong_to_ky')
        CREATE INDEX IX_chi_so_cong_to_ky ON dbo.chi_so_cong_to(ky_ghi, cong_to_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.hoa_don') AND name = N'IX_hoa_don_han_trang_thai')
        CREATE INDEX IX_hoa_don_han_trang_thai ON dbo.hoa_don(han_thanh_toan, trang_thai);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chi_tiet_hoa_don') AND name = N'IX_chi_tiet_hoa_don_hoa_don')
        CREATE INDEX IX_chi_tiet_hoa_don_hoa_don ON dbo.chi_tiet_hoa_don(hoa_don_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.thanh_toan') AND name = N'IX_thanh_toan_nguoi_thue_trang_thai')
        CREATE INDEX IX_thanh_toan_nguoi_thue_trang_thai ON dbo.thanh_toan(nguoi_thue_id, trang_thai);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.phan_bo_thanh_toan') AND name = N'IX_phan_bo_thanh_toan_hoa_don')
        CREATE INDEX IX_phan_bo_thanh_toan_hoa_don ON dbo.phan_bo_thanh_toan(hoa_don_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chi_phi') AND name = N'IX_chi_phi_ngay_trang_thai')
        CREATE INDEX IX_chi_phi_ngay_trang_thai ON dbo.chi_phi(ngay_chi, trang_thai);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.yeu_cau_sua_chua') AND name = N'IX_yeu_cau_sua_chua_phong_trang_thai')
        CREATE INDEX IX_yeu_cau_sua_chua_phong_trang_thai ON dbo.yeu_cau_sua_chua(phong_tro_id, trang_thai);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.kiem_tra_phong') AND name = N'IX_kiem_tra_phong_ngay')
        CREATE INDEX IX_kiem_tra_phong_ngay ON dbo.kiem_tra_phong(phong_tro_id, ngay_kiem_tra);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.thong_bao') AND name = N'IX_thong_bao_nguoi_dung_doc')
        CREATE INDEX IX_thong_bao_nguoi_dung_doc ON dbo.thong_bao(nguoi_dung_id, da_doc, created_at);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.nhat_ky_he_thong') AND name = N'IX_nhat_ky_he_thong_created_at')
        CREATE INDEX IX_nhat_ky_he_thong_created_at ON dbo.nhat_ky_he_thong(created_at);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.dat_phong') AND name = N'IX_dat_phong_phong_ngay_trang_thai')
        CREATE INDEX IX_dat_phong_phong_ngay_trang_thai ON dbo.dat_phong(phong_tro_id, ngay_nhan_phong_du_kien, trang_thai);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.hop_dong_thue') AND name = N'UX_hop_dong_thue_one_active_per_room')
        CREATE UNIQUE INDEX UX_hop_dong_thue_one_active_per_room ON dbo.hop_dong_thue(phong_tro_id) WHERE trang_thai = N'HIEU_LUC';
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.hop_dong_nguoi_thue') AND name = N'UX_hop_dong_nguoi_thue_one_active_representative')
        CREATE UNIQUE INDEX UX_hop_dong_nguoi_thue_one_active_representative ON dbo.hop_dong_nguoi_thue(hop_dong_thue_id) WHERE la_dai_dien = 1 AND ngay_roi_di IS NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.cong_to') AND name = N'UX_cong_to_one_active_meter_per_service_room')
        CREATE UNIQUE INDEX UX_cong_to_one_active_meter_per_service_room ON dbo.cong_to(phong_tro_id, dich_vu_id) WHERE trang_thai = N'DANG_SU_DUNG';
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.lich_su_dieu_chinh_hop_dong') AND name = N'IX_lich_su_hop_dong_contract_created')
        CREATE INDEX IX_lich_su_hop_dong_contract_created ON dbo.lich_su_dieu_chinh_hop_dong(hop_dong_thue_id, created_at DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.tien_nghi_phong') AND name = N'IX_tien_nghi_phong_danh_muc')
        CREATE INDEX IX_tien_nghi_phong_danh_muc ON dbo.tien_nghi_phong(danh_muc_tien_nghi_id, phong_tro_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chi_tiet_hoa_don') AND name = N'IX_chi_tiet_hoa_don_dich_vu_hop_dong')
        CREATE INDEX IX_chi_tiet_hoa_don_dich_vu_hop_dong ON dbo.chi_tiet_hoa_don(dich_vu_hop_dong_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chi_tiet_hoa_don') AND name = N'IX_chi_tiet_hoa_don_chi_so_cong_to')
        CREATE INDEX IX_chi_tiet_hoa_don_chi_so_cong_to ON dbo.chi_tiet_hoa_don(chi_so_cong_to_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.dieu_chinh_hoa_don') AND name = N'IX_dieu_chinh_hoa_don_invoice_created')
        CREATE INDEX IX_dieu_chinh_hoa_don_invoice_created ON dbo.dieu_chinh_hoa_don(hoa_don_id, created_at DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.vat_tu_sua_chua') AND name = N'IX_vat_tu_sua_chua_nha_cung_cap')
        CREATE INDEX IX_vat_tu_sua_chua_nha_cung_cap ON dbo.vat_tu_sua_chua(nha_cung_cap_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.tai_san_phong') AND name = N'IX_tai_san_phong_phong')
        CREATE INDEX IX_tai_san_phong_phong ON dbo.tai_san_phong(phong_tro_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.bien_ban_ban_giao_tai_san') AND name = N'IX_bien_ban_ban_giao_hop_dong_nguoi_thue')
        CREATE INDEX IX_bien_ban_ban_giao_hop_dong_nguoi_thue ON dbo.bien_ban_ban_giao_tai_san(hop_dong_thue_id, nguoi_thue_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.kiem_tra_phong') AND name = N'IX_kiem_tra_phong_hop_dong')
        CREATE INDEX IX_kiem_tra_phong_hop_dong ON dbo.kiem_tra_phong(hop_dong_thue_id, ngay_kiem_tra);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chi_tiet_kiem_tra_phong') AND name = N'IX_chi_tiet_kiem_tra_phong_kiem_tra_thu_tu')
        CREATE INDEX IX_chi_tiet_kiem_tra_phong_kiem_tra_thu_tu ON dbo.chi_tiet_kiem_tra_phong(kiem_tra_phong_id, thu_tu);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.nhat_ky_he_thong') AND name = N'IX_nhat_ky_he_thong_nguoi_dung_created')
        CREATE INDEX IX_nhat_ky_he_thong_nguoi_dung_created ON dbo.nhat_ky_he_thong(nguoi_dung_id, created_at DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.dat_phong') AND name = N'IX_dat_phong_nguoi_dung_created')
        CREATE INDEX IX_dat_phong_nguoi_dung_created ON dbo.dat_phong(nguoi_dung_id, created_at DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.hop_dong_thue') AND name = N'IX_hop_dong_thue_created_by')
        CREATE INDEX IX_hop_dong_thue_created_by ON dbo.hop_dong_thue(created_by);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.lich_su_dieu_chinh_hop_dong') AND name = N'IX_lich_su_dieu_chinh_hop_dong_nguoi_dung')
        CREATE INDEX IX_lich_su_dieu_chinh_hop_dong_nguoi_dung ON dbo.lich_su_dieu_chinh_hop_dong(nguoi_dung_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.dich_vu_hop_dong') AND name = N'IX_dich_vu_hop_dong_dich_vu')
        CREATE INDEX IX_dich_vu_hop_dong_dich_vu ON dbo.dich_vu_hop_dong(dich_vu_id, hop_dong_thue_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.cong_to') AND name = N'IX_cong_to_dich_vu')
        CREATE INDEX IX_cong_to_dich_vu ON dbo.cong_to(dich_vu_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chi_so_cong_to') AND name = N'IX_chi_so_cong_to_nguoi_ghi')
        CREATE INDEX IX_chi_so_cong_to_nguoi_ghi ON dbo.chi_so_cong_to(nguoi_ghi_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.dieu_chinh_hoa_don') AND name = N'IX_dieu_chinh_hoa_don_nguoi_dung')
        CREATE INDEX IX_dieu_chinh_hoa_don_nguoi_dung ON dbo.dieu_chinh_hoa_don(nguoi_dung_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chi_phi') AND name = N'IX_chi_phi_nha_cung_cap')
        CREATE INDEX IX_chi_phi_nha_cung_cap ON dbo.chi_phi(nha_cung_cap_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chi_phi') AND name = N'IX_chi_phi_phong')
        CREATE INDEX IX_chi_phi_phong ON dbo.chi_phi(phong_tro_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chi_phi') AND name = N'IX_chi_phi_nguoi_tao')
        CREATE INDEX IX_chi_phi_nguoi_tao ON dbo.chi_phi(nguoi_tao_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.yeu_cau_sua_chua') AND name = N'IX_yeu_cau_sua_chua_nguoi_thue')
        CREATE INDEX IX_yeu_cau_sua_chua_nguoi_thue ON dbo.yeu_cau_sua_chua(nguoi_thue_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.yeu_cau_sua_chua') AND name = N'IX_yeu_cau_sua_chua_nguoi_phu_trach')
        CREATE INDEX IX_yeu_cau_sua_chua_nguoi_phu_trach ON dbo.yeu_cau_sua_chua(nguoi_phu_trach_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.vat_tu_sua_chua') AND name = N'IX_vat_tu_sua_chua_yeu_cau')
        CREATE INDEX IX_vat_tu_sua_chua_yeu_cau ON dbo.vat_tu_sua_chua(yeu_cau_sua_chua_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.bien_ban_ban_giao_tai_san') AND name = N'IX_bien_ban_ban_giao_tai_san')
        CREATE INDEX IX_bien_ban_ban_giao_tai_san ON dbo.bien_ban_ban_giao_tai_san(tai_san_phong_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.bien_ban_ban_giao_tai_san') AND name = N'IX_bien_ban_ban_giao_nguoi_thue')
        CREATE INDEX IX_bien_ban_ban_giao_nguoi_thue ON dbo.bien_ban_ban_giao_tai_san(nguoi_thue_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.kiem_tra_phong') AND name = N'IX_kiem_tra_phong_nguoi_kiem_tra')
        CREATE INDEX IX_kiem_tra_phong_nguoi_kiem_tra ON dbo.kiem_tra_phong(nguoi_kiem_tra_id);

    IF (SELECT COUNT_BIG(*)
        FROM (VALUES
            (N'vai_tro'), (N'nguoi_dung'), (N'khu_tro'), (N'loai_phong'), (N'phong_tro'), (N'nguoi_thue'),
            (N'hop_dong_thue'), (N'hop_dong_nguoi_thue'), (N'lich_su_dieu_chinh_hop_dong'), (N'dich_vu'),
            (N'dich_vu_hop_dong'), (N'danh_muc_tien_nghi'), (N'tien_nghi_phong'), (N'cong_to'), (N'chi_so_cong_to'),
            (N'hoa_don'), (N'chi_tiet_hoa_don'), (N'dieu_chinh_hoa_don'), (N'thanh_toan'), (N'phan_bo_thanh_toan'),
            (N'nha_cung_cap'), (N'chi_phi'), (N'yeu_cau_sua_chua'), (N'vat_tu_sua_chua'), (N'tai_san_phong'),
            (N'bien_ban_ban_giao_tai_san'), (N'kiem_tra_phong'), (N'chi_tiet_kiem_tra_phong'), (N'thong_bao'),
            (N'nhat_ky_he_thong'), (N'dat_phong')
        ) AS targets(table_name)
        WHERE OBJECT_ID(N'dbo.' + targets.table_name, N'U') IS NOT NULL) <> 31
        THROW 50013, 'Business schema verification failed; rolling back the migration.', 1;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
