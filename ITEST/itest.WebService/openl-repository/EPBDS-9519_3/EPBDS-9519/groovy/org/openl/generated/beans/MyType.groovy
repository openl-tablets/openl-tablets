package org.openl.generated.beans

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

@XmlRootElement(namespace = "http://beans.generated.openl.org", name = "MyType")
@XmlType(namespace = "http://beans.generated.openl.org", name = "MyType", propOrder = ["mYSPR1", "ySrp2", "MYSPR"])
@EqualsAndHashCode
@ToString
class MyType {
    private String mYSPR1;
    private String ySrp2;
    private String MYSPR;

    MyType() {
    }

    MyType(String var1, String var2, String var3) {
        this.mYSPR1 = var1;
        this.ySrp2 = var2;
        this.MYSPR = var3;
    }

    @XmlElement(name = "mYSPR1")
    String getmYSPR1() {
        return this.mYSPR1;
    }

    @XmlElement(name = "ySrp2")
    String getySrp2() {
        return this.ySrp2;
    }

    @XmlElement(name = "MYSPR")
    String getMYSPR() {
        return this.MYSPR;
    }

    void setmYSPR1(String mYSPR1) {
        this.mYSPR1 = mYSPR1;
    }

    void setySrp2(String ySrp2) {
        this.ySrp2 = ySrp2;
    }

    void setMYSPR(String MYSPR) {
        this.MYSPR = MYSPR;
    }
}
