SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF DB_NAME() <> N'QuanLyPhongTro'
    THROW 50030, 'Refusing to apply room media schema outside QuanLyPhongTro.', 1;

IF OBJECT_ID(N'dbo.phong_tro', N'U') IS NULL
    THROW 50031, 'Required dbo.phong_tro table is missing.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @ApplicationLockResult INT;
    EXEC @ApplicationLockResult = sys.sp_getapplock
        @Resource = N'QuanLyPhongTro:009_room_media',
        @LockMode = N'Exclusive',
        @LockOwner = N'Transaction',
        @LockTimeout = 10000;
    IF @ApplicationLockResult < 0
        THROW 50032, 'Could not obtain the room media schema lock.', 1;

    IF OBJECT_ID(N'dbo.hinh_anh_video_phong', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.hinh_anh_video_phong
        (
            media_id BIGINT IDENTITY(1,1) NOT NULL,
            phong_tro_id BIGINT NOT NULL,
            loai_file NVARCHAR(10) NOT NULL,
            ten_file_goc NVARCHAR(255) NOT NULL,
            ten_file_luu NVARCHAR(100) NOT NULL,
            duong_dan NVARCHAR(1000) NOT NULL,
            mime_type NVARCHAR(127) NOT NULL,
            kich_thuoc_bytes BIGINT NOT NULL,
            thu_tu_hien_thi INT NOT NULL CONSTRAINT DF_media_thu_tu DEFAULT 0,
            la_anh_dai_dien BIT NOT NULL CONSTRAINT DF_media_anh_dai_dien DEFAULT 0,
            mo_ta NVARCHAR(500) NULL,
            nguoi_tai_len_id BIGINT NULL,
            ngay_tai_len DATETIME2(0) NOT NULL CONSTRAINT DF_media_ngay_tai_len DEFAULT SYSUTCDATETIME(),
            CONSTRAINT PK_hinh_anh_video_phong PRIMARY KEY (media_id),
            CONSTRAINT FK_media_phong FOREIGN KEY (phong_tro_id) REFERENCES dbo.phong_tro(phong_tro_id),
            CONSTRAINT FK_media_nguoi_tai_len FOREIGN KEY (nguoi_tai_len_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id),
            CONSTRAINT CK_media_loai_file CHECK (loai_file IN (N'IMAGE', N'VIDEO')),
            CONSTRAINT CK_media_kich_thuoc CHECK (kich_thuoc_bytes > 0),
            CONSTRAINT CK_media_thu_tu CHECK (thu_tu_hien_thi >= 0),
            CONSTRAINT CK_media_anh_dai_dien CHECK (la_anh_dai_dien = 0 OR loai_file = N'IMAGE'),
            CONSTRAINT UQ_media_duong_dan UNIQUE (duong_dan)
        );
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.hinh_anh_video_phong') AND name = N'IX_media_phong_thu_tu')
        CREATE INDEX IX_media_phong_thu_tu
            ON dbo.hinh_anh_video_phong(phong_tro_id, thu_tu_hien_thi, media_id);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.hinh_anh_video_phong') AND name = N'UX_media_mot_anh_dai_dien_moi_phong')
        CREATE UNIQUE INDEX UX_media_mot_anh_dai_dien_moi_phong
            ON dbo.hinh_anh_video_phong(phong_tro_id)
            WHERE la_anh_dai_dien = 1;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
