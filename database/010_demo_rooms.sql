SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF DB_NAME() <> N'QuanLyPhongTro'
    THROW 50100, 'Refusing to apply demo data outside QuanLyPhongTro.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @ApplicationLockResult INT;
    EXEC @ApplicationLockResult = sys.sp_getapplock
        @Resource = N'QuanLyPhongTro:010_demo_rooms',
        @LockMode = N'Exclusive',
        @LockOwner = N'Transaction',
        @LockTimeout = 10000;
    IF @ApplicationLockResult < 0
        THROW 50101, 'Could not obtain the demo data lock.', 1;

    DECLARE @AdminId BIGINT;
    DECLARE @TenantUserId BIGINT;
    DECLARE @TenantId BIGINT;
    DECLARE @SiteId BIGINT;
    DECLARE @ContractId BIGINT;
    DECLARE @InvoiceId BIGINT;
    DECLARE @ElectricServiceId BIGINT;
    DECLARE @WaterServiceId BIGINT;
    DECLARE @InternetServiceId BIGINT;
    DECLARE @ElectricMeterId BIGINT;
    DECLARE @WaterMeterId BIGINT;
    DECLARE @ElectricReadingId BIGINT;
    DECLARE @WaterReadingId BIGINT;
    DECLARE @ContractStart DATE = '20261001';
    DECLARE @ContractEnd DATE = '20270930';
    DECLARE @InvoicePeriod DATE = '20261101';

    IF (
        SELECT COUNT_BIG(*)
        FROM dbo.nguoi_dung u
        JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
        WHERE u.ten_dang_nhap = N'admin'
          AND u.trang_thai = N'HOAT_DONG'
          AND r.ma_vai_tro = N'ADMIN'
    ) <> 1
        THROW 50102, 'Expected exactly one active ADMIN account named admin.', 1;

    SELECT @AdminId = u.nguoi_dung_id
    FROM dbo.nguoi_dung u
    JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
    WHERE u.ten_dang_nhap = N'admin'
      AND u.trang_thai = N'HOAT_DONG'
      AND r.ma_vai_tro = N'ADMIN';

    IF (
        SELECT COUNT_BIG(*)
        FROM dbo.nguoi_dung u
        JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
        WHERE u.ten_dang_nhap = N'user'
          AND u.trang_thai = N'HOAT_DONG'
          AND r.ma_vai_tro = N'NGUOI_THUE'
    ) <> 1
        THROW 50103, 'Expected exactly one active NGUOI_THUE account named user.', 1;

    SELECT @TenantUserId = u.nguoi_dung_id
    FROM dbo.nguoi_dung u
    JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
    WHERE u.ten_dang_nhap = N'user'
      AND u.trang_thai = N'HOAT_DONG'
      AND r.ma_vai_tro = N'NGUOI_THUE';

    IF (SELECT COUNT_BIG(*) FROM dbo.nguoi_thue WHERE nguoi_dung_id = @TenantUserId) <> 1
        THROW 50104, 'The existing user account does not have exactly one linked tenant profile.', 1;

    SELECT @TenantId = nguoi_thue_id
    FROM dbo.nguoi_thue
    WHERE nguoi_dung_id = @TenantUserId;

    IF EXISTS (
        SELECT 1
        FROM dbo.khu_tro k
        JOIN dbo.phong_tro p ON p.khu_tro_id = k.khu_tro_id
        WHERE p.ma_phong IN (N'P101', N'P102', N'P103', N'P104', N'P105', N'P106')
          AND k.ma_khu_tro <> N'DEMO-KHU-TRO-01'
    )
        THROW 50105, 'A target room code already belongs to a non-demo site; refusing to move or overwrite it.', 1;

    DECLARE @DemoSite TABLE
    (
        ma_khu_tro NVARCHAR(60) NOT NULL PRIMARY KEY,
        ten_khu_tro NVARCHAR(300) NOT NULL,
        dia_chi NVARCHAR(1000) NOT NULL,
        mo_ta NVARCHAR(2000) NULL,
        trang_thai NVARCHAR(40) NOT NULL
    );
    INSERT INTO @DemoSite
    VALUES (
        N'DEMO-KHU-TRO-01',
        N'[DEMO] Khu trọ An Bình',
        N'Đường số 8, Phường Linh Trung, TP. Thủ Đức, TP. Hồ Chí Minh (địa chỉ mẫu)',
        N'[DEMO] Khu trọ mẫu phục vụ trình diễn website quản lý phòng trọ.',
        N'HOAT_DONG'
    );

    SELECT @SiteId = khu_tro_id
    FROM dbo.khu_tro WITH (UPDLOCK, HOLDLOCK)
    WHERE ma_khu_tro = N'DEMO-KHU-TRO-01';

    IF @SiteId IS NULL
    BEGIN
        INSERT INTO dbo.khu_tro (ma_khu_tro, ten_khu_tro, dia_chi, mo_ta, trang_thai)
        SELECT ma_khu_tro, ten_khu_tro, dia_chi, mo_ta, trang_thai
        FROM @DemoSite;
        SET @SiteId = CONVERT(BIGINT, SCOPE_IDENTITY());
    END
    ELSE
    BEGIN
        UPDATE k
        SET k.ten_khu_tro = s.ten_khu_tro,
            k.dia_chi = s.dia_chi,
            k.mo_ta = s.mo_ta,
            k.trang_thai = s.trang_thai,
            k.updated_at = SYSUTCDATETIME()
        FROM dbo.khu_tro k
        JOIN @DemoSite s ON s.ma_khu_tro = k.ma_khu_tro;
    END;

    CREATE TABLE #RoomTypes
    (
        ma_loai_phong NVARCHAR(60) NOT NULL PRIMARY KEY,
        ten_loai_phong NVARCHAR(240) NOT NULL,
        dien_tich_m2 DECIMAL(8,2) NOT NULL,
        gia_thue_mac_dinh DECIMAL(19,2) NOT NULL,
        mo_ta NVARCHAR(2000) NULL
    );
    INSERT INTO #RoomTypes
    VALUES
        (N'DEMO-LOAI-P101', N'[DEMO] Phòng bình dân', 18, 1800000, N'Phòng cơ bản, gọn gàng.'),
        (N'DEMO-LOAI-P102', N'[DEMO] Phòng tiêu chuẩn nhỏ', 20, 2000000, N'Phòng tiêu chuẩn, phù hợp một đến hai người.'),
        (N'DEMO-LOAI-P103', N'[DEMO] Phòng tiêu chuẩn', 22, 2300000, N'Phòng tiêu chuẩn có không gian sinh hoạt riêng.'),
        (N'DEMO-LOAI-P104', N'[DEMO] Phòng có gác', 25, 2700000, N'Phòng có gác xép, tận dụng diện tích.'),
        (N'DEMO-LOAI-P105', N'[DEMO] Phòng cao cấp', 28, 3200000, N'Phòng rộng, trang bị tiện nghi cơ bản.'),
        (N'DEMO-LOAI-P106', N'[DEMO] Phòng rộng ban công', 32, 3800000, N'Phòng rộng, có ban công.');

    UPDATE l
    SET l.ten_loai_phong = t.ten_loai_phong,
        l.dien_tich_m2 = t.dien_tich_m2,
        l.gia_thue_mac_dinh = t.gia_thue_mac_dinh,
        l.mo_ta = t.mo_ta,
        l.trang_thai = N'HOAT_DONG',
        l.updated_at = SYSUTCDATETIME()
    FROM dbo.loai_phong l
    JOIN #RoomTypes t ON t.ma_loai_phong = l.ma_loai_phong;

    INSERT INTO dbo.loai_phong
        (ma_loai_phong, ten_loai_phong, dien_tich_m2, gia_thue_mac_dinh, mo_ta, trang_thai)
    SELECT t.ma_loai_phong, t.ten_loai_phong, t.dien_tich_m2, t.gia_thue_mac_dinh, t.mo_ta, N'HOAT_DONG'
    FROM #RoomTypes t
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.loai_phong l WITH (UPDLOCK, HOLDLOCK)
        WHERE l.ma_loai_phong = t.ma_loai_phong
    );

    CREATE TABLE #Rooms
    (
        ma_phong NVARCHAR(60) NOT NULL PRIMARY KEY,
        ma_loai_phong NVARCHAR(60) NOT NULL,
        tang NVARCHAR(40) NULL,
        dien_tich_m2 DECIMAL(8,2) NOT NULL,
        gia_thue DECIMAL(19,2) NOT NULL,
        trang_thai NVARCHAR(48) NOT NULL,
        mo_ta NVARCHAR(2000) NOT NULL
    );
    INSERT INTO #Rooms
    VALUES
        (N'P101', N'DEMO-LOAI-P101', N'Tầng 1', 18, 1800000, N'DANG_THUE', N'[DEMO] Phòng bình dân; đang thuê theo hợp đồng mẫu.'),
        (N'P102', N'DEMO-LOAI-P102', N'Tầng 1', 20, 2000000, N'TRONG', N'[DEMO] Phòng tiêu chuẩn nhỏ, hiện còn trống.'),
        (N'P103', N'DEMO-LOAI-P103', N'Tầng 1', 22, 2300000, N'TRONG', N'[DEMO] Phòng tiêu chuẩn, hiện còn trống.'),
        (N'P104', N'DEMO-LOAI-P104', N'Tầng 2', 25, 2700000, N'TRONG', N'[DEMO] Phòng có gác xép, hiện còn trống.'),
        (N'P105', N'DEMO-LOAI-P105', N'Tầng 2', 28, 3200000, N'TRONG', N'[DEMO] Phòng cao cấp, hiện còn trống.'),
        (N'P106', N'DEMO-LOAI-P106', N'Tầng 2', 32, 3800000, N'TRONG', N'[DEMO] Phòng rộng có ban công, hiện còn trống.');

    IF EXISTS (
        SELECT 1
        FROM dbo.hop_dong_thue h
        JOIN dbo.phong_tro p ON p.phong_tro_id = h.phong_tro_id
        WHERE p.khu_tro_id = @SiteId
          AND p.ma_phong IN (N'P102', N'P103', N'P104', N'P105', N'P106')
          AND h.trang_thai = N'HIEU_LUC'
    )
        THROW 50106, 'A vacant target room already has an active lease; refusing to overwrite the lease.', 1;

    UPDATE p
    SET p.loai_phong_id = l.loai_phong_id,
        p.tang = r.tang,
        p.dien_tich_m2 = r.dien_tich_m2,
        p.gia_thue = r.gia_thue,
        p.trang_thai = r.trang_thai,
        p.mo_ta = r.mo_ta,
        p.updated_at = SYSUTCDATETIME()
    FROM dbo.phong_tro p
    JOIN #Rooms r ON r.ma_phong = p.ma_phong
    JOIN dbo.loai_phong l ON l.ma_loai_phong = r.ma_loai_phong
    WHERE p.khu_tro_id = @SiteId;

    INSERT INTO dbo.phong_tro
        (khu_tro_id, loai_phong_id, ma_phong, tang, dien_tich_m2, gia_thue, trang_thai, mo_ta)
    SELECT @SiteId, l.loai_phong_id, r.ma_phong, r.tang, r.dien_tich_m2, r.gia_thue, r.trang_thai, r.mo_ta
    FROM #Rooms r
    JOIN dbo.loai_phong l ON l.ma_loai_phong = r.ma_loai_phong
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.phong_tro p WITH (UPDLOCK, HOLDLOCK)
        WHERE p.khu_tro_id = @SiteId AND p.ma_phong = r.ma_phong
    );

    CREATE TABLE #Amenities
    (
        ma_tien_nghi NVARCHAR(80) NOT NULL PRIMARY KEY,
        ten_tien_nghi NVARCHAR(300) NOT NULL,
        mo_ta NVARCHAR(2000) NULL
    );
    INSERT INTO #Amenities
    VALUES
        (N'DEMO-GIUONG', N'Giường', N'Giường ngủ trong phòng.'),
        (N'DEMO-TU-QUAN-AO', N'Tủ quần áo', N'Tủ lưu trữ quần áo.'),
        (N'DEMO-DIEU-HOA', N'Điều hòa', N'Máy điều hòa không khí.'),
        (N'DEMO-NONG-LANH', N'Bình nóng lạnh', N'Bình nước nóng.'),
        (N'DEMO-WC-RIENG', N'Nhà vệ sinh riêng', N'Nhà vệ sinh khép kín.'),
        (N'DEMO-WIFI', N'Wifi', N'Kết nối internet không dây.'),
        (N'DEMO-CHO-DE-XE', N'Chỗ để xe', N'Khu vực gửi xe trong khu trọ.'),
        (N'DEMO-BAN-CONG', N'Ban công', N'Ban công riêng.'),
        (N'DEMO-GAC-XEP', N'Gác xép', N'Gác xép tăng diện tích sử dụng.');

    UPDATE a
    SET a.ten_tien_nghi = d.ten_tien_nghi,
        a.mo_ta = d.mo_ta,
        a.trang_thai = N'HOAT_DONG'
    FROM dbo.danh_muc_tien_nghi a
    JOIN #Amenities d ON d.ma_tien_nghi = a.ma_tien_nghi;

    INSERT INTO dbo.danh_muc_tien_nghi (ma_tien_nghi, ten_tien_nghi, mo_ta, trang_thai)
    SELECT d.ma_tien_nghi, d.ten_tien_nghi, d.mo_ta, N'HOAT_DONG'
    FROM #Amenities d
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.danh_muc_tien_nghi a WITH (UPDLOCK, HOLDLOCK)
        WHERE a.ma_tien_nghi = d.ma_tien_nghi
    );

    CREATE TABLE #RoomAmenities
    (
        ma_phong NVARCHAR(60) NOT NULL,
        ma_tien_nghi NVARCHAR(80) NOT NULL,
        so_luong INT NOT NULL,
        tinh_trang NVARCHAR(80) NULL
    );
    INSERT INTO #RoomAmenities
    VALUES
        (N'P101', N'DEMO-GIUONG', 1, N'TOT'),
        (N'P101', N'DEMO-TU-QUAN-AO', 1, N'TOT'),
        (N'P101', N'DEMO-DIEU-HOA', 1, N'TOT'),
        (N'P101', N'DEMO-WC-RIENG', 1, N'TOT'),
        (N'P101', N'DEMO-WIFI', 1, N'TOT'),
        (N'P102', N'DEMO-GIUONG', 1, N'TOT'),
        (N'P102', N'DEMO-TU-QUAN-AO', 1, N'TOT'),
        (N'P102', N'DEMO-WC-RIENG', 1, N'TOT'),
        (N'P102', N'DEMO-WIFI', 1, N'TOT'),
        (N'P102', N'DEMO-CHO-DE-XE', 1, N'TOT'),
        (N'P103', N'DEMO-GIUONG', 1, N'TOT'),
        (N'P103', N'DEMO-TU-QUAN-AO', 1, N'TOT'),
        (N'P103', N'DEMO-DIEU-HOA', 1, N'TOT'),
        (N'P103', N'DEMO-NONG-LANH', 1, N'TOT'),
        (N'P103', N'DEMO-WC-RIENG', 1, N'TOT'),
        (N'P103', N'DEMO-WIFI', 1, N'TOT'),
        (N'P104', N'DEMO-GIUONG', 1, N'TOT'),
        (N'P104', N'DEMO-TU-QUAN-AO', 1, N'TOT'),
        (N'P104', N'DEMO-GAC-XEP', 1, N'TOT'),
        (N'P104', N'DEMO-WC-RIENG', 1, N'TOT'),
        (N'P104', N'DEMO-WIFI', 1, N'TOT'),
        (N'P105', N'DEMO-GIUONG', 1, N'TOT'),
        (N'P105', N'DEMO-TU-QUAN-AO', 1, N'TOT'),
        (N'P105', N'DEMO-DIEU-HOA', 1, N'TOT'),
        (N'P105', N'DEMO-NONG-LANH', 1, N'TOT'),
        (N'P105', N'DEMO-WC-RIENG', 1, N'TOT'),
        (N'P105', N'DEMO-WIFI', 1, N'TOT'),
        (N'P105', N'DEMO-BAN-CONG', 1, N'TOT'),
        (N'P106', N'DEMO-GIUONG', 1, N'TOT'),
        (N'P106', N'DEMO-TU-QUAN-AO', 1, N'TOT'),
        (N'P106', N'DEMO-DIEU-HOA', 1, N'TOT'),
        (N'P106', N'DEMO-NONG-LANH', 1, N'TOT'),
        (N'P106', N'DEMO-WC-RIENG', 1, N'TOT'),
        (N'P106', N'DEMO-WIFI', 1, N'TOT'),
        (N'P106', N'DEMO-BAN-CONG', 1, N'TOT'),
        (N'P106', N'DEMO-CHO-DE-XE', 1, N'TOT');

    UPDATE rp
    SET rp.so_luong = ra.so_luong,
        rp.tinh_trang = ra.tinh_trang,
        rp.ghi_chu = N'[DEMO] Tiện nghi mẫu.'
    FROM dbo.tien_nghi_phong rp
    JOIN dbo.phong_tro p ON p.phong_tro_id = rp.phong_tro_id
    JOIN dbo.danh_muc_tien_nghi a ON a.danh_muc_tien_nghi_id = rp.danh_muc_tien_nghi_id
    JOIN #RoomAmenities ra ON ra.ma_phong = p.ma_phong AND ra.ma_tien_nghi = a.ma_tien_nghi
    WHERE p.khu_tro_id = @SiteId;

    INSERT INTO dbo.tien_nghi_phong (phong_tro_id, danh_muc_tien_nghi_id, so_luong, tinh_trang, ghi_chu)
    SELECT p.phong_tro_id, a.danh_muc_tien_nghi_id, ra.so_luong, ra.tinh_trang, N'[DEMO] Tiện nghi mẫu.'
    FROM #RoomAmenities ra
    JOIN dbo.phong_tro p ON p.khu_tro_id = @SiteId AND p.ma_phong = ra.ma_phong
    JOIN dbo.danh_muc_tien_nghi a ON a.ma_tien_nghi = ra.ma_tien_nghi
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.tien_nghi_phong rp WITH (UPDLOCK, HOLDLOCK)
        WHERE rp.phong_tro_id = p.phong_tro_id
          AND rp.danh_muc_tien_nghi_id = a.danh_muc_tien_nghi_id
    );

    CREATE TABLE #RoomAssets
    (
        ma_phong NVARCHAR(60) NOT NULL,
        ma_tai_san NVARCHAR(80) NOT NULL PRIMARY KEY,
        ten_tai_san NVARCHAR(400) NOT NULL,
        so_luong INT NOT NULL,
        nguyen_gia DECIMAL(19,2) NULL,
        tinh_trang NVARCHAR(80) NOT NULL,
        ghi_chu NVARCHAR(2000) NULL
    );
    INSERT INTO #RoomAssets
    VALUES
        (N'P101', N'DEMO-P101-GIUONG', N'Giường đơn', 1, 1800000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P101', N'DEMO-P101-TU', N'Tủ quần áo', 1, 1200000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P101', N'DEMO-P101-DIEU-HOA', N'Điều hòa', 1, 6500000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P102', N'DEMO-P102-GIUONG', N'Giường đơn', 1, 1800000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P102', N'DEMO-P102-TU', N'Tủ quần áo', 1, 1200000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P103', N'DEMO-P103-GIUONG', N'Giường đôi', 1, 2500000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P103', N'DEMO-P103-TU', N'Tủ quần áo', 1, 1500000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P103', N'DEMO-P103-DIEU-HOA', N'Điều hòa', 1, 7000000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P104', N'DEMO-P104-GIUONG', N'Giường đôi', 1, 2500000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P104', N'DEMO-P104-TU', N'Tủ quần áo', 1, 1500000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P105', N'DEMO-P105-GIUONG', N'Giường đôi', 1, 2800000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P105', N'DEMO-P105-TU', N'Tủ quần áo', 1, 1800000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P105', N'DEMO-P105-DIEU-HOA', N'Điều hòa', 1, 7500000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P106', N'DEMO-P106-GIUONG', N'Giường đôi', 1, 3000000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P106', N'DEMO-P106-TU', N'Tủ quần áo', 1, 1800000, N'TOT', N'[DEMO] Tài sản mẫu.'),
        (N'P106', N'DEMO-P106-DIEU-HOA', N'Điều hòa', 1, 8000000, N'TOT', N'[DEMO] Tài sản mẫu.');

    UPDATE a
    SET a.phong_tro_id = p.phong_tro_id,
        a.ten_tai_san = s.ten_tai_san,
        a.so_luong = s.so_luong,
        a.nguyen_gia = s.nguyen_gia,
        a.tinh_trang = s.tinh_trang,
        a.ghi_chu = s.ghi_chu,
        a.updated_at = SYSUTCDATETIME()
    FROM dbo.tai_san_phong a
    JOIN #RoomAssets s ON s.ma_tai_san = a.ma_tai_san
    JOIN dbo.phong_tro p ON p.khu_tro_id = @SiteId AND p.ma_phong = s.ma_phong;

    INSERT INTO dbo.tai_san_phong
        (phong_tro_id, ma_tai_san, ten_tai_san, so_luong, ngay_mua, nguyen_gia, tinh_trang, ghi_chu)
    SELECT p.phong_tro_id, s.ma_tai_san, s.ten_tai_san, s.so_luong, '20250115',
           s.nguyen_gia, s.tinh_trang, s.ghi_chu
    FROM #RoomAssets s
    JOIN dbo.phong_tro p ON p.khu_tro_id = @SiteId AND p.ma_phong = s.ma_phong
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.tai_san_phong a WITH (UPDLOCK, HOLDLOCK)
        WHERE a.ma_tai_san = s.ma_tai_san
    );

    CREATE TABLE #Services
    (
        ma_dich_vu NVARCHAR(80) NOT NULL PRIMARY KEY,
        ten_dich_vu NVARCHAR(300) NOT NULL,
        don_vi_tinh NVARCHAR(80) NOT NULL,
        don_gia DECIMAL(19,2) NOT NULL,
        cach_tinh NVARCHAR(48) NOT NULL,
        mo_ta NVARCHAR(2000) NULL
    );
    INSERT INTO #Services
    VALUES
        (N'DEMO-DIEN', N'[DEMO] Điện', N'kWh', 3500, N'THEO_CHI_SO', N'Đơn giá điện mẫu.'),
        (N'DEMO-NUOC', N'[DEMO] Nước', N'm³', 18000, N'THEO_CHI_SO', N'Đơn giá nước mẫu.'),
        (N'DEMO-INTERNET', N'[DEMO] Internet', N'tháng', 120000, N'CO_DINH', N'Phí internet mẫu theo tháng.');

    UPDATE d
    SET d.ten_dich_vu = s.ten_dich_vu,
        d.don_vi_tinh = s.don_vi_tinh,
        d.don_gia = s.don_gia,
        d.cach_tinh = s.cach_tinh,
        d.trang_thai = N'HOAT_DONG',
        d.mo_ta = s.mo_ta,
        d.updated_at = SYSUTCDATETIME()
    FROM dbo.dich_vu d
    JOIN #Services s ON s.ma_dich_vu = d.ma_dich_vu;

    INSERT INTO dbo.dich_vu (ma_dich_vu, ten_dich_vu, don_vi_tinh, don_gia, cach_tinh, trang_thai, mo_ta)
    SELECT s.ma_dich_vu, s.ten_dich_vu, s.don_vi_tinh, s.don_gia, s.cach_tinh, N'HOAT_DONG', s.mo_ta
    FROM #Services s
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.dich_vu d WITH (UPDLOCK, HOLDLOCK)
        WHERE d.ma_dich_vu = s.ma_dich_vu
    );

    SELECT @ElectricServiceId = dich_vu_id FROM dbo.dich_vu WHERE ma_dich_vu = N'DEMO-DIEN';
    SELECT @WaterServiceId = dich_vu_id FROM dbo.dich_vu WHERE ma_dich_vu = N'DEMO-NUOC';
    SELECT @InternetServiceId = dich_vu_id FROM dbo.dich_vu WHERE ma_dich_vu = N'DEMO-INTERNET';

    IF EXISTS (
        SELECT 1
        FROM dbo.hop_dong_thue h
        JOIN dbo.hop_dong_nguoi_thue hn ON hn.hop_dong_thue_id = h.hop_dong_thue_id
        WHERE hn.nguoi_thue_id = @TenantId
          AND hn.ngay_roi_di IS NULL
          AND h.trang_thai = N'HIEU_LUC'
          AND h.ma_hop_dong <> N'DEMO-HD-P101-2026'
    )
        THROW 50107, 'The existing tenant already belongs to another active lease; refusing to create a conflicting lease.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.hop_dong_thue h
        JOIN dbo.phong_tro p ON p.phong_tro_id = h.phong_tro_id
        WHERE p.khu_tro_id = @SiteId
          AND p.ma_phong = N'P101'
          AND h.trang_thai = N'HIEU_LUC'
          AND h.ma_hop_dong <> N'DEMO-HD-P101-2026'
    )
        THROW 50108, 'P101 already has a different active lease; refusing to overwrite it.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.hop_dong_thue h
        JOIN dbo.phong_tro p ON p.phong_tro_id = h.phong_tro_id
        WHERE p.khu_tro_id = @SiteId
          AND h.ma_hop_dong = N'DEMO-HD-P101-2026'
          AND p.ma_phong <> N'P101'
    )
        THROW 50109, 'The demo contract code is already attached to another room.', 1;

    SELECT @ContractId = hop_dong_thue_id
    FROM dbo.hop_dong_thue WITH (UPDLOCK, HOLDLOCK)
    WHERE ma_hop_dong = N'DEMO-HD-P101-2026';

    IF @ContractId IS NULL
    BEGIN
        INSERT INTO dbo.hop_dong_thue
            (phong_tro_id, ma_hop_dong, ngay_bat_dau, ngay_ket_thuc, gia_thue,
             tien_coc, ngay_thanh_toan_hang_thang, trang_thai, ghi_chu, created_by)
        SELECT p.phong_tro_id, N'DEMO-HD-P101-2026', @ContractStart, @ContractEnd, 1800000,
               3600000, 5, N'HIEU_LUC', N'[DEMO] Hợp đồng mẫu của tài khoản user.', @AdminId
        FROM dbo.phong_tro p
        WHERE p.khu_tro_id = @SiteId AND p.ma_phong = N'P101';
        SET @ContractId = CONVERT(BIGINT, SCOPE_IDENTITY());
    END
    ELSE
    BEGIN
        UPDATE dbo.hop_dong_thue
        SET ngay_bat_dau = @ContractStart,
            ngay_ket_thuc = @ContractEnd,
            gia_thue = 1800000,
            tien_coc = 3600000,
            ngay_thanh_toan_hang_thang = 5,
            trang_thai = N'HIEU_LUC',
            ghi_chu = N'[DEMO] Hợp đồng mẫu của tài khoản user.',
            created_by = @AdminId,
            updated_at = SYSUTCDATETIME()
        WHERE hop_dong_thue_id = @ContractId;
    END;

    IF EXISTS (
        SELECT 1
        FROM dbo.hop_dong_nguoi_thue hn
        WHERE hn.hop_dong_thue_id = @ContractId
          AND hn.ngay_roi_di IS NULL
          AND hn.nguoi_thue_id <> @TenantId
    )
        THROW 50110, 'The demo contract is linked to a different active tenant; refusing to replace the tenant.', 1;

    UPDATE dbo.hop_dong_nguoi_thue
    SET la_dai_dien = 1,
        ngay_tham_gia = @ContractStart,
        ngay_roi_di = NULL
    WHERE hop_dong_thue_id = @ContractId
      AND nguoi_thue_id = @TenantId;

    IF @@ROWCOUNT = 0
        INSERT INTO dbo.hop_dong_nguoi_thue
            (hop_dong_thue_id, nguoi_thue_id, la_dai_dien, ngay_tham_gia, ngay_roi_di)
        VALUES (@ContractId, @TenantId, 1, @ContractStart, NULL);

    IF EXISTS (
        SELECT 1
        FROM dbo.dich_vu_hop_dong existing
        JOIN dbo.dich_vu d ON d.dich_vu_id = existing.dich_vu_id
        WHERE existing.hop_dong_thue_id = @ContractId
          AND d.ma_dich_vu IN (N'DEMO-DIEN', N'DEMO-NUOC', N'DEMO-INTERNET')
          AND existing.ngay_bat_dau <> @ContractStart
    )
        THROW 50111, 'A demo contract service has a conflicting start date.', 1;

    UPDATE dh
    SET dh.don_gia_ap_dung = s.don_gia,
        dh.ngay_ket_thuc = NULL
    FROM dbo.dich_vu_hop_dong dh
    JOIN dbo.dich_vu d ON d.dich_vu_id = dh.dich_vu_id
    JOIN #Services s ON s.ma_dich_vu = d.ma_dich_vu
    WHERE dh.hop_dong_thue_id = @ContractId
      AND dh.ngay_bat_dau = @ContractStart;

    INSERT INTO dbo.dich_vu_hop_dong
        (hop_dong_thue_id, dich_vu_id, don_gia_ap_dung, ngay_bat_dau, ngay_ket_thuc)
    SELECT @ContractId, d.dich_vu_id, s.don_gia, @ContractStart, NULL
    FROM #Services s
    JOIN dbo.dich_vu d ON d.ma_dich_vu = s.ma_dich_vu
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.dich_vu_hop_dong dh WITH (UPDLOCK, HOLDLOCK)
        WHERE dh.hop_dong_thue_id = @ContractId
          AND dh.dich_vu_id = d.dich_vu_id
          AND dh.ngay_bat_dau = @ContractStart
    );

    IF EXISTS (
        SELECT 1
        FROM dbo.cong_to m
        JOIN dbo.phong_tro p ON p.phong_tro_id = m.phong_tro_id
        JOIN dbo.dich_vu d ON d.dich_vu_id = m.dich_vu_id
        WHERE p.khu_tro_id = @SiteId AND p.ma_phong = N'P101'
          AND d.ma_dich_vu = N'DEMO-DIEN'
          AND m.trang_thai = N'DANG_SU_DUNG'
          AND m.ma_cong_to <> N'DEMO-P101-DIEN'
    )
        THROW 50112, 'P101 already has a different active electricity meter.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.cong_to m
        JOIN dbo.phong_tro p ON p.phong_tro_id = m.phong_tro_id
        JOIN dbo.dich_vu d ON d.dich_vu_id = m.dich_vu_id
        WHERE p.khu_tro_id = @SiteId AND p.ma_phong = N'P101'
          AND d.ma_dich_vu = N'DEMO-NUOC'
          AND m.trang_thai = N'DANG_SU_DUNG'
          AND m.ma_cong_to <> N'DEMO-P101-NUOC'
    )
        THROW 50113, 'P101 already has a different active water meter.', 1;

    UPDATE m
    SET m.phong_tro_id = p.phong_tro_id,
        m.dich_vu_id = d.dich_vu_id,
        m.ngay_lap = @ContractStart,
        m.ngay_thao = NULL,
        m.chi_so_dau = 0,
        m.trang_thai = N'DANG_SU_DUNG',
        m.ghi_chu = N'[DEMO] Công tơ mẫu.'
    FROM dbo.cong_to m
    JOIN dbo.phong_tro p ON p.khu_tro_id = @SiteId AND p.ma_phong = N'P101'
    JOIN dbo.dich_vu d ON d.ma_dich_vu = N'DEMO-DIEN'
    WHERE m.ma_cong_to = N'DEMO-P101-DIEN';

    IF @@ROWCOUNT = 0
    BEGIN
        INSERT INTO dbo.cong_to
            (phong_tro_id, dich_vu_id, ma_cong_to, ngay_lap, ngay_thao, chi_so_dau, trang_thai, ghi_chu)
        SELECT p.phong_tro_id, d.dich_vu_id, N'DEMO-P101-DIEN', @ContractStart, NULL, 0, N'DANG_SU_DUNG',
               N'[DEMO] Công tơ điện mẫu.'
        FROM dbo.phong_tro p
        JOIN dbo.dich_vu d ON d.ma_dich_vu = N'DEMO-DIEN'
        WHERE p.khu_tro_id = @SiteId AND p.ma_phong = N'P101';
    END;

    UPDATE m
    SET m.phong_tro_id = p.phong_tro_id,
        m.dich_vu_id = d.dich_vu_id,
        m.ngay_lap = @ContractStart,
        m.ngay_thao = NULL,
        m.chi_so_dau = 0,
        m.trang_thai = N'DANG_SU_DUNG',
        m.ghi_chu = N'[DEMO] Công tơ mẫu.'
    FROM dbo.cong_to m
    JOIN dbo.phong_tro p ON p.khu_tro_id = @SiteId AND p.ma_phong = N'P101'
    JOIN dbo.dich_vu d ON d.ma_dich_vu = N'DEMO-NUOC'
    WHERE m.ma_cong_to = N'DEMO-P101-NUOC';

    IF @@ROWCOUNT = 0
    BEGIN
        INSERT INTO dbo.cong_to
            (phong_tro_id, dich_vu_id, ma_cong_to, ngay_lap, ngay_thao, chi_so_dau, trang_thai, ghi_chu)
        SELECT p.phong_tro_id, d.dich_vu_id, N'DEMO-P101-NUOC', @ContractStart, NULL, 0, N'DANG_SU_DUNG',
               N'[DEMO] Công tơ nước mẫu.'
        FROM dbo.phong_tro p
        JOIN dbo.dich_vu d ON d.ma_dich_vu = N'DEMO-NUOC'
        WHERE p.khu_tro_id = @SiteId AND p.ma_phong = N'P101';
    END;

    SELECT @ElectricMeterId = cong_to_id FROM dbo.cong_to WHERE ma_cong_to = N'DEMO-P101-DIEN';
    SELECT @WaterMeterId = cong_to_id FROM dbo.cong_to WHERE ma_cong_to = N'DEMO-P101-NUOC';

    UPDATE dbo.chi_so_cong_to
    SET chi_so = 120,
        nguoi_ghi_id = @AdminId,
        ghi_chu = N'[DEMO] Chỉ số điện tháng 11/2026, mức tiêu thụ mẫu 120 kWh.'
    WHERE cong_to_id = @ElectricMeterId AND ky_ghi = @InvoicePeriod;
    IF @@ROWCOUNT = 0
        INSERT INTO dbo.chi_so_cong_to (cong_to_id, ky_ghi, chi_so, nguoi_ghi_id, ghi_chu)
        VALUES (@ElectricMeterId, @InvoicePeriod, 120, @AdminId,
                N'[DEMO] Chỉ số điện tháng 11/2026, mức tiêu thụ mẫu 120 kWh.');

    UPDATE dbo.chi_so_cong_to
    SET chi_so = 8,
        nguoi_ghi_id = @AdminId,
        ghi_chu = N'[DEMO] Chỉ số nước tháng 11/2026, mức tiêu thụ mẫu 8 m³.'
    WHERE cong_to_id = @WaterMeterId AND ky_ghi = @InvoicePeriod;
    IF @@ROWCOUNT = 0
        INSERT INTO dbo.chi_so_cong_to (cong_to_id, ky_ghi, chi_so, nguoi_ghi_id, ghi_chu)
        VALUES (@WaterMeterId, @InvoicePeriod, 8, @AdminId,
                N'[DEMO] Chỉ số nước tháng 11/2026, mức tiêu thụ mẫu 8 m³.');

    SELECT @ElectricReadingId = chi_so_cong_to_id
    FROM dbo.chi_so_cong_to
    WHERE cong_to_id = @ElectricMeterId AND ky_ghi = @InvoicePeriod;
    SELECT @WaterReadingId = chi_so_cong_to_id
    FROM dbo.chi_so_cong_to
    WHERE cong_to_id = @WaterMeterId AND ky_ghi = @InvoicePeriod;

    SELECT @InvoiceId = hoa_don_id
    FROM dbo.hoa_don WITH (UPDLOCK, HOLDLOCK)
    WHERE hop_dong_thue_id = @ContractId AND ky_hoa_don = @InvoicePeriod;

    IF @InvoiceId IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM dbo.hoa_don
        WHERE hoa_don_id = @InvoiceId
          AND LEFT(ghi_chu, 6) = N'[DEMO]'
    )
        THROW 50114, 'A non-demo invoice already exists for the demo contract and period; refusing to overwrite it.', 1;

    IF @InvoiceId IS NULL
    BEGIN
        INSERT INTO dbo.hoa_don
            (hop_dong_thue_id, ky_hoa_don, ngay_phat_hanh, han_thanh_toan, tong_tien, trang_thai, ghi_chu)
        VALUES (@ContractId, @InvoicePeriod, '20261102', '20261110', 2484000,
                N'CHUA_THANH_TOAN', N'[DEMO] Hóa đơn mẫu P101 kỳ 11/2026.');
        SET @InvoiceId = CONVERT(BIGINT, SCOPE_IDENTITY());
    END
    ELSE
    BEGIN
        UPDATE dbo.hoa_don
        SET ngay_phat_hanh = '20261102',
            han_thanh_toan = '20261110',
            tong_tien = 2484000,
            trang_thai = N'CHUA_THANH_TOAN',
            ghi_chu = N'[DEMO] Hóa đơn mẫu P101 kỳ 11/2026.',
            updated_at = SYSUTCDATETIME()
        WHERE hoa_don_id = @InvoiceId;
    END;

    CREATE TABLE #InvoiceLines
    (
        marker NVARCHAR(100) NOT NULL PRIMARY KEY,
        ten_khoan_thu NVARCHAR(400) NOT NULL,
        don_vi_tinh NVARCHAR(80) NULL,
        so_luong DECIMAL(19,3) NOT NULL,
        don_gia DECIMAL(19,2) NOT NULL,
        thanh_tien DECIMAL(19,2) NOT NULL,
        dich_vu_hop_dong_id BIGINT NULL,
        chi_so_cong_to_id BIGINT NULL
    );
    INSERT INTO #InvoiceLines
    VALUES
        (N'[DEMO] P101 RENT', N'Tiền thuê phòng', N'tháng', 1, 1800000, 1800000, NULL, NULL),
        (N'[DEMO] P101 ELECTRIC', N'Tiền điện', N'kWh', 120, 3500, 420000,
            (SELECT dh.dich_vu_hop_dong_id FROM dbo.dich_vu_hop_dong dh WHERE dh.hop_dong_thue_id = @ContractId AND dh.dich_vu_id = @ElectricServiceId AND dh.ngay_bat_dau = @ContractStart),
            @ElectricReadingId),
        (N'[DEMO] P101 WATER', N'Tiền nước', N'm³', 8, 18000, 144000,
            (SELECT dh.dich_vu_hop_dong_id FROM dbo.dich_vu_hop_dong dh WHERE dh.hop_dong_thue_id = @ContractId AND dh.dich_vu_id = @WaterServiceId AND dh.ngay_bat_dau = @ContractStart),
            @WaterReadingId),
        (N'[DEMO] P101 INTERNET', N'Internet', N'tháng', 1, 120000, 120000,
            (SELECT dh.dich_vu_hop_dong_id FROM dbo.dich_vu_hop_dong dh WHERE dh.hop_dong_thue_id = @ContractId AND dh.dich_vu_id = @InternetServiceId AND dh.ngay_bat_dau = @ContractStart),
            NULL);

    IF (SELECT COUNT_BIG(*) FROM #InvoiceLines WHERE dich_vu_hop_dong_id IS NOT NULL) <> 3
        THROW 50115, 'Could not resolve all contract service rows for the P101 invoice.', 1;

    IF EXISTS (
        SELECT 1
        FROM #InvoiceLines l
        JOIN dbo.chi_tiet_hoa_don d ON d.hoa_don_id = @InvoiceId AND d.ghi_chu = l.marker
        GROUP BY l.marker
        HAVING COUNT_BIG(*) > 1
    )
        THROW 50116, 'Duplicate demo invoice detail markers were found.', 1;

    UPDATE d
    SET d.ten_khoan_thu = l.ten_khoan_thu,
        d.don_vi_tinh = l.don_vi_tinh,
        d.so_luong = l.so_luong,
        d.don_gia = l.don_gia,
        d.thanh_tien = l.thanh_tien,
        d.dich_vu_hop_dong_id = l.dich_vu_hop_dong_id,
        d.chi_so_cong_to_id = l.chi_so_cong_to_id
    FROM dbo.chi_tiet_hoa_don d
    JOIN #InvoiceLines l ON l.marker = d.ghi_chu
    WHERE d.hoa_don_id = @InvoiceId;

    INSERT INTO dbo.chi_tiet_hoa_don
        (hoa_don_id, dich_vu_hop_dong_id, chi_so_cong_to_id, ten_khoan_thu,
         don_vi_tinh, so_luong, don_gia, thanh_tien, ghi_chu)
    SELECT @InvoiceId, l.dich_vu_hop_dong_id, l.chi_so_cong_to_id, l.ten_khoan_thu,
           l.don_vi_tinh, l.so_luong, l.don_gia, l.thanh_tien, l.marker
    FROM #InvoiceLines l
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.chi_tiet_hoa_don d WITH (UPDLOCK, HOLDLOCK)
        WHERE d.hoa_don_id = @InvoiceId AND d.ghi_chu = l.marker
    );

    IF EXISTS (
        SELECT 1
        FROM dbo.hop_dong_thue h
        JOIN dbo.phong_tro p ON p.phong_tro_id = h.phong_tro_id
        WHERE p.khu_tro_id = @SiteId
          AND p.ma_phong IN (N'P102', N'P103', N'P104', N'P105', N'P106')
          AND h.trang_thai = N'HIEU_LUC'
    )
        THROW 50117, 'A vacant demo room has an active lease.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.hoa_don i
        JOIN dbo.hop_dong_thue h ON h.hop_dong_thue_id = i.hop_dong_thue_id
        JOIN dbo.phong_tro p ON p.phong_tro_id = h.phong_tro_id
        WHERE p.khu_tro_id = @SiteId
          AND p.ma_phong IN (N'P102', N'P103', N'P104', N'P105', N'P106')
    )
        THROW 50118, 'A vacant demo room has an invoice; refusing to claim the room is clean.', 1;

    IF (SELECT COUNT_BIG(*) FROM dbo.phong_tro WHERE khu_tro_id = @SiteId AND ma_phong IN (N'P101',N'P102',N'P103',N'P104',N'P105',N'P106')) <> 6
        THROW 50119, 'The demo site does not contain exactly the six requested room codes.', 1;

    IF (SELECT COUNT_BIG(*) FROM dbo.hop_dong_nguoi_thue WHERE hop_dong_thue_id = @ContractId AND nguoi_thue_id = @TenantId AND ngay_roi_di IS NULL AND la_dai_dien = 1) <> 1
        THROW 50120, 'The active tenant is not linked as the contract representative.', 1;

    IF (SELECT COUNT_BIG(*) FROM dbo.hop_dong_thue WHERE hop_dong_thue_id = @ContractId AND trang_thai = N'HIEU_LUC') <> 1
        THROW 50121, 'The P101 demo contract is not active.', 1;

    IF (SELECT COUNT_BIG(*) FROM dbo.chi_tiet_hoa_don WHERE hoa_don_id = @InvoiceId AND ghi_chu LIKE N'[[]DEMO] P101 %') <> 4
        THROW 50122, 'The P101 demo invoice does not have the four expected detail rows.', 1;

    COMMIT TRANSACTION;

    SELECT DB_NAME() AS database_name, N'COMMITTED' AS transaction_status;
    SELECT p.ma_phong, p.dien_tich_m2, p.gia_thue, p.trang_thai,
           k.ma_khu_tro, l.ma_loai_phong
    FROM dbo.phong_tro p
    JOIN dbo.khu_tro k ON k.khu_tro_id = p.khu_tro_id
    JOIN dbo.loai_phong l ON l.loai_phong_id = p.loai_phong_id
    WHERE k.ma_khu_tro = N'DEMO-KHU-TRO-01'
      AND p.ma_phong IN (N'P101',N'P102',N'P103',N'P104',N'P105',N'P106')
    ORDER BY p.ma_phong;

    SELECT u.ten_dang_nhap, r.ma_vai_tro, u.trang_thai,
           h.ma_hop_dong, h.trang_thai AS contract_status, p.ma_phong,
           i.ky_hoa_don, i.tong_tien, i.trang_thai AS invoice_status
    FROM dbo.hop_dong_thue h
    JOIN dbo.phong_tro p ON p.phong_tro_id = h.phong_tro_id
    JOIN dbo.hop_dong_nguoi_thue hn ON hn.hop_dong_thue_id = h.hop_dong_thue_id
    JOIN dbo.nguoi_thue nt ON nt.nguoi_thue_id = hn.nguoi_thue_id
    JOIN dbo.nguoi_dung u ON u.nguoi_dung_id = nt.nguoi_dung_id
    JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
    LEFT JOIN dbo.hoa_don i ON i.hop_dong_thue_id = h.hop_dong_thue_id AND i.ky_hoa_don = @InvoicePeriod
    WHERE h.ma_hop_dong = N'DEMO-HD-P101-2026';

    SELECT
        (SELECT COUNT_BIG(*) FROM dbo.khu_tro WHERE ma_khu_tro = N'DEMO-KHU-TRO-01') AS demo_sites,
        (SELECT COUNT_BIG(*) FROM dbo.loai_phong WHERE ma_loai_phong LIKE N'DEMO-LOAI-P10_') AS demo_room_types,
        (SELECT COUNT_BIG(*) FROM dbo.phong_tro p JOIN dbo.khu_tro k ON k.khu_tro_id=p.khu_tro_id WHERE k.ma_khu_tro=N'DEMO-KHU-TRO-01' AND p.ma_phong LIKE N'P10_') AS demo_rooms,
        (SELECT COUNT_BIG(*) FROM dbo.tien_nghi_phong rp JOIN dbo.phong_tro p ON p.phong_tro_id=rp.phong_tro_id WHERE p.khu_tro_id=@SiteId) AS room_amenities,
        (SELECT COUNT_BIG(*) FROM dbo.tai_san_phong a JOIN dbo.phong_tro p ON p.phong_tro_id=a.phong_tro_id WHERE p.khu_tro_id=@SiteId AND a.ma_tai_san LIKE N'DEMO-P10%') AS room_assets,
        (SELECT COUNT_BIG(*) FROM dbo.cong_to m JOIN dbo.phong_tro p ON p.phong_tro_id=m.phong_tro_id WHERE p.khu_tro_id=@SiteId AND m.ma_cong_to LIKE N'DEMO-P101-%') AS utility_meters,
        (SELECT COUNT_BIG(*) FROM dbo.chi_so_cong_to r JOIN dbo.cong_to m ON m.cong_to_id=r.cong_to_id WHERE m.ma_cong_to LIKE N'DEMO-P101-%' AND r.ky_ghi=@InvoicePeriod) AS meter_readings,
        (SELECT COUNT_BIG(*) FROM dbo.hoa_don WHERE hoa_don_id=@InvoiceId) AS demo_invoices,
        (SELECT COUNT_BIG(*) FROM dbo.chi_tiet_hoa_don WHERE hoa_don_id=@InvoiceId AND ghi_chu LIKE N'[[]DEMO] P101 %') AS demo_invoice_lines,
        (SELECT COUNT_BIG(*) FROM dbo.hop_dong_thue h JOIN dbo.phong_tro p ON p.phong_tro_id=h.phong_tro_id WHERE p.khu_tro_id=@SiteId AND p.ma_phong IN(N'P102',N'P103',N'P104',N'P105',N'P106') AND h.trang_thai=N'HIEU_LUC') AS active_leases_on_vacant_rooms,
        (SELECT COUNT_BIG(*) FROM dbo.hoa_don i JOIN dbo.hop_dong_thue h ON h.hop_dong_thue_id=i.hop_dong_thue_id JOIN dbo.phong_tro p ON p.phong_tro_id=h.phong_tro_id WHERE p.khu_tro_id=@SiteId AND p.ma_phong IN(N'P102',N'P103',N'P104',N'P105',N'P106')) AS invoices_on_vacant_rooms;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
