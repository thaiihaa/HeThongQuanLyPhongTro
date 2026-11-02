package com.quanlyphongtro.health;

import com.quanlyphongtro.health.dto.DatabaseHealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class DatabaseHealthService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthService.class);
    private static final String EXPECTED_DATABASE = "QuanLyPhongTro";
    private static final Pattern SECRET_PROPERTY = Pattern.compile(
            "(?i)(password|pwd|accessToken|clientSecret|secret)(\\s*[=:]\\s*)([^\\r\\n]*)");

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DatabaseHealthResponse check() {
        try {
            String databaseName = jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class);
            if (!EXPECTED_DATABASE.equals(databaseName)) {
                log.error("Health query connected to unexpected database: expected={}, actual={}",
                        EXPECTED_DATABASE, databaseName);
                return DatabaseHealthResponse.down("DATABASE_MISMATCH");
            }
            return DatabaseHealthResponse.up(databaseName);
        } catch (CannotGetJdbcConnectionException ex) {
            log.error("Không thể kết nối SQL Server:\n{}", sanitizedStackTrace(ex));
            return DatabaseHealthResponse.down(classifyConnectionError(ex));
        } catch (DataAccessException ex) {
            log.error("Lỗi truy vấn kiểm tra database:\n{}", sanitizedStackTrace(ex));
            return DatabaseHealthResponse.down(classifyDataAccessError(ex));
        }
    }

    private String classifyConnectionError(CannotGetJdbcConnectionException ex) {
        Throwable root = rootCause(ex);
        String message = root.getMessage() != null ? root.getMessage().toLowerCase(Locale.ROOT) : "";

        if (message.contains("driver") || message.contains("integrated security")
                || message.contains("authentication dll") || message.contains("mssql-jdbc_auth")) {
            return "DRIVER_OR_INTEGRATED_AUTH_ERROR";
        }
        if (message.contains("login failed") || message.contains("authentication")) {
            return "AUTHENTICATION_FAILED";
        }
        if (message.contains("cannot open database") || message.contains("database") && message.contains("not found")) {
            return "DATABASE_NOT_FOUND";
        }
        if (message.contains("connection refused") || message.contains("network") || message.contains("timed out")) {
            return "NETWORK_ERROR";
        }
        if (message.contains("instance") || message.contains("server was not found")) {
            return "INSTANCE_OR_SERVER_NOT_FOUND";
        }
        return "CONNECTION_FAILED";
    }

    private String classifyDataAccessError(DataAccessException ex) {
        Throwable root = rootCause(ex);
        if (root instanceof SQLException sqlEx) {
            int code = sqlEx.getErrorCode();
            if (code == 18456) {
                return "AUTHENTICATION_FAILED";
            }
            if (code == 4060) {
                return "DATABASE_NOT_FOUND";
            }
        }
        return "QUERY_FAILED";
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String sanitizedStackTrace(Throwable throwable) {
        StringWriter trace = new StringWriter();
        throwable.printStackTrace(new PrintWriter(trace));
        return SECRET_PROPERTY.matcher(trace.toString()).replaceAll("$1$2[REDACTED]");
    }
}
