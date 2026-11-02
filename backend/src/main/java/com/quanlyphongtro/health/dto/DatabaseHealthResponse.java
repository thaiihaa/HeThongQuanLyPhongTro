package com.quanlyphongtro.health.dto;

public record DatabaseHealthResponse(
        String status,
        String database,
        String errorCode,
        String message
) {
    public static DatabaseHealthResponse up(String database) {
        return new DatabaseHealthResponse("UP", database, null, null);
    }

    public static DatabaseHealthResponse down(String errorCode) {
        return new DatabaseHealthResponse("DOWN", null, errorCode,
                "Không thể kết nối hoặc truy vấn database. Xem log backend để biết chi tiết.");
    }
}
