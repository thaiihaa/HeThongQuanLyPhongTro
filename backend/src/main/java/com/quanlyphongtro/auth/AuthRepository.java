package com.quanlyphongtro.auth;

import com.quanlyphongtro.auth.AuthModels.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class AuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> findRoleId(String roleCode) {
        List<Long> matches = jdbcTemplate.query(
                "SELECT vai_tro_id FROM dbo.vai_tro WHERE ma_vai_tro = ?",
                (resultSet, rowNum) -> resultSet.getLong("vai_tro_id"),
                roleCode
        );
        return matches.stream().findFirst();
    }

    public Optional<Long> findUserIdByUsername(String username) {
        List<Long> matches = jdbcTemplate.query(
                "SELECT nguoi_dung_id FROM dbo.nguoi_dung WHERE ten_dang_nhap = ?",
                (resultSet, rowNum) -> resultSet.getLong("nguoi_dung_id"),
                username
        );
        return matches.stream().findFirst();
    }

    public Optional<String> findPasswordHash(long userId) {
        List<String> hashes = jdbcTemplate.query(
                "SELECT mat_khau_hash FROM dbo.nguoi_dung WHERE nguoi_dung_id = ?",
                (resultSet, rowNum) -> resultSet.getString("mat_khau_hash"),
                userId
        );
        return hashes.stream().findFirst();
    }

    public int updatePasswordHash(long userId, String currentPasswordHash, String passwordHash) {
        return jdbcTemplate.update("""
                UPDATE dbo.nguoi_dung
                SET mat_khau_hash = ?, updated_at = SYSUTCDATETIME()
                WHERE nguoi_dung_id = ? AND mat_khau_hash = ?
                """, passwordHash, userId, currentPasswordHash);
    }

    public void revokeAllActiveSessions(long userId) {
        jdbcTemplate.update("""
                UPDATE dbo.phien_dang_nhap
                SET revoked_at = SYSUTCDATETIME()
                WHERE nguoi_dung_id = ? AND revoked_at IS NULL
                """, userId);
    }

    public Optional<AccountRecord> findAccountByLogin(String login) {
        List<AccountRecord> accounts = jdbcTemplate.query("""
                SELECT TOP (1) u.nguoi_dung_id, u.ten_dang_nhap, u.mat_khau_hash,
                       u.ho_ten, u.trang_thai, r.ma_vai_tro
                FROM dbo.nguoi_dung u
                JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
                WHERE u.ten_dang_nhap = ? OR u.email = ?
                """,
                (resultSet, rowNum) -> new AccountRecord(
                        resultSet.getLong("nguoi_dung_id"),
                        resultSet.getString("ten_dang_nhap"),
                        resultSet.getString("mat_khau_hash"),
                        resultSet.getString("ho_ten"),
                        resultSet.getString("trang_thai"),
                        resultSet.getString("ma_vai_tro")
                ),
                login,
                login
        );
        return accounts.stream().findFirst();
    }

    public Optional<AuthenticatedUser> findActiveUserByTokenHash(String tokenHash) {
        List<AuthenticatedUser> users = jdbcTemplate.query("""
                SELECT TOP (1) u.nguoi_dung_id, u.ten_dang_nhap, u.ho_ten, r.ma_vai_tro
                FROM dbo.phien_dang_nhap s
                JOIN dbo.nguoi_dung u ON u.nguoi_dung_id = s.nguoi_dung_id
                JOIN dbo.vai_tro r ON r.vai_tro_id = u.vai_tro_id
                WHERE s.token_hash = ?
                  AND s.revoked_at IS NULL
                  AND s.expires_at > SYSUTCDATETIME()
                  AND u.trang_thai = N'HOAT_DONG'
                """,
                (resultSet, rowNum) -> new AuthenticatedUser(
                        resultSet.getLong("nguoi_dung_id"),
                        resultSet.getString("ten_dang_nhap"),
                        resultSet.getString("ho_ten"),
                        resultSet.getString("ma_vai_tro")
                ),
                tokenHash
        );
        return users.stream().findFirst();
    }

    public long insertUser(long roleId, String username, String email, String phone, String passwordHash, String fullName) {
        Long userId = jdbcTemplate.queryForObject("""
                INSERT INTO dbo.nguoi_dung
                    (vai_tro_id, ten_dang_nhap, email, so_dien_thoai, mat_khau_hash, ho_ten)
                OUTPUT INSERTED.nguoi_dung_id
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                Long.class,
                roleId,
                username,
                email,
                phone,
                passwordHash,
                fullName
        );
        if (userId == null) {
            throw new IllegalStateException("SQL Server did not return the new user ID.");
        }
        return userId;
    }

    public void insertTenantProfile(long userId, String citizenId, java.time.LocalDate birthDate, String address) {
        jdbcTemplate.update("""
                INSERT INTO dbo.nguoi_thue (nguoi_dung_id, cccd, ngay_sinh, dia_chi)
                VALUES (?, ?, ?, ?)
                """,
                userId,
                citizenId,
                birthDate == null ? null : Date.valueOf(birthDate),
                address
        );
    }

    public void insertSession(long userId, String tokenHash, Instant expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO dbo.phien_dang_nhap (nguoi_dung_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """,
                userId,
                tokenHash,
                Timestamp.from(expiresAt)
        );
    }

    public void revokeSession(String tokenHash) {
        jdbcTemplate.update("""
                UPDATE dbo.phien_dang_nhap
                SET revoked_at = SYSUTCDATETIME()
                WHERE token_hash = ? AND revoked_at IS NULL
                """,
                tokenHash
        );
    }

    public record AccountRecord(
            long id,
            String username,
            String passwordHash,
            String fullName,
            String status,
            String role
    ) {
    }
}
