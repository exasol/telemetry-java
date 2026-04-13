package com.exasol.telemetry;

import java.io.IOException;

final class HttpException extends IOException {
    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String serverStatus;

    HttpException(final int statusCode, final String serverStatus) {
        super(serverStatus);
        this.statusCode = statusCode;
        this.serverStatus = serverStatus;
    }

    int getStatusCode() {
        return statusCode;
    }

    String getServerStatus() {
        return serverStatus;
    }
}
