package com.cangxu.wms.common;

import java.util.UUID;

public record Result<T>(int code, String message, T data, String traceId) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "ok", data, trace());
    }

    public static Result<Void> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, trace());
    }

    private static String trace() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
