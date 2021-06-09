package org.openl.itest.serviceclass.internal

class Response {
    private String status;
    private int code;

    Response() {
    }

    Response(String status, int code) {
        this.status = status;
        this.code = code;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    int getCode() {
        return code;
    }

    void setCode(int code) {
        this.code = code;
    }
}
