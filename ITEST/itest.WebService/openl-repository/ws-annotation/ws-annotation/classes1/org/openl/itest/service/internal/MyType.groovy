package org.openl.itest.service.internal

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlRootElement

@XmlAccessorType( XmlAccessType.FIELD )
@XmlRootElement
class MyType {

    private String status;
    private int code;

    MyType() {
    }

    MyType(String status, int code) {
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
