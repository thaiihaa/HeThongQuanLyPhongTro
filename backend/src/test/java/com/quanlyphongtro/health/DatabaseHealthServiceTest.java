package com.quanlyphongtro.health;

import org.junit.jupiter.api.Test;
import com.quanlyphongtro.health.dto.DatabaseHealthResponse;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseHealthServiceTest {

    @Test
    void reportsUpWhenExpectedDatabaseIsConnected() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class))
                .thenReturn("QuanLyPhongTro");

        DatabaseHealthResponse response = new DatabaseHealthService(jdbcTemplate).check();

        assertEquals("UP", response.status());
        assertEquals("QuanLyPhongTro", response.database());
    }

    @Test
    void reportsDownWhenConnectedToUnexpectedDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class))
                .thenReturn("master");

        DatabaseHealthResponse response = new DatabaseHealthService(jdbcTemplate).check();

        assertEquals("DOWN", response.status());
        assertEquals("DATABASE_MISMATCH", response.errorCode());
        assertNull(response.database());
    }

    @Test
    void classifiesMissingIntegratedAuthenticationAsDriverError() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SQLException cause = new SQLException("This driver is not configured for integrated authentication.");
        when(jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class))
                .thenThrow(new CannotGetJdbcConnectionException("Unable to connect", cause));

        DatabaseHealthResponse response = new DatabaseHealthService(jdbcTemplate).check();

        assertEquals("DRIVER_OR_INTEGRATED_AUTH_ERROR", response.errorCode());
    }
}
