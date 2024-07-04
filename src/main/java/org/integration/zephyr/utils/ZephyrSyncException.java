package org.integration.zephyr.utils;

public class ZephyrSyncException extends RuntimeException {

    public ZephyrSyncException(String message) {
        super(message);
    }

    public ZephyrSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
