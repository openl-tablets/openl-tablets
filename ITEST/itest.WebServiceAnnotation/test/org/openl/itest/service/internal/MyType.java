package org.openl.itest.service.internal;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MyType {

    private String status;
    private int code;

    public MyType() {
    }

    public MyType(String status, int code) {
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
