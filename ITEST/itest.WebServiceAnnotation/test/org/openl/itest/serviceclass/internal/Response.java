package org.openl.itest.serviceclass.internal;

public class Response {

    private String status;
    private int code;

    public Response() {
    }

    public Response(String status, int code) {
        this.status = status;
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

}
