package com.exigen.le;

public class LiveExcelException extends RuntimeException {
    
    public LiveExcelException() {
        super();
    }

    public LiveExcelException(String message) {
        super(message);
    }

    public LiveExcelException(String message, Throwable cause) {
        super(message, cause);
    }

    public LiveExcelException(Throwable cause) {
        super(cause);
    }

}
