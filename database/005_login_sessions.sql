SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF DB_NAME() <> N'QuanLyPhongTro'
    THROW 50002, 'Refusing to apply login session schema outside QuanLyPhongTro.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.phien_dang_nhap', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.phien_dang_nhap
        (
            phien_dang_nhap_id BIGINT IDENTITY(1,1) NOT NULL,
            nguoi_dung_id BIGINT NOT NULL,
            token_hash CHAR(64) NOT NULL,
            expires_at DATETIME2(0) NOT NULL,
            created_at DATETIME2(0) NOT NULL CONSTRAINT DF_phien_dang_nhap_created_at DEFAULT SYSUTCDATETIME(),
            revoked_at DATETIME2(0) NULL,
            CONSTRAINT PK_phien_dang_nhap PRIMARY KEY (phien_dang_nhap_id),
            CONSTRAINT UQ_phien_dang_nhap_token_hash UNIQUE (token_hash),
            CONSTRAINT FK_phien_dang_nhap_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES dbo.nguoi_dung(nguoi_dung_id)
        );

        CREATE INDEX IX_phien_dang_nhap_nguoi_dung_expires
            ON dbo.phien_dang_nhap(nguoi_dung_id, expires_at, revoked_at);
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;