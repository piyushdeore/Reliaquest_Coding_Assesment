package com.reliaquest.api.exception;

public class EmployeeNotFoundException extends RuntimeException{
    private final String errorCode;

    public EmployeeNotFoundException(String errorCode, Throwable cause) {
        super(errorCode, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public EmployeeNotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }
}
