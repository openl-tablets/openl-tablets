package org.openl.generated.epbds6830;

import java.util.Objects;

import javax.xml.bind.annotation.XmlTransient;

public class BeanA {
    private int a = 1;
    protected int B = 2;
    private int aa = 4;
    protected int aB = 8;
    @XmlTransient
    protected transient int Ba = 16;
    protected int xB = 64;

    public BeanA() {
    }

    public int getA() {
        return a;
    }

    protected void setA(int a) {
        this.a = a;
    }

    public int getB() {
        return B;
    }

    public void setB(int b) {
        B = b;
    }

    public int getAa() {
        return aa;
    }

    public void setAa(int aa) {
        this.aa = aa;
    }

    private int getAB() {
        return aB;
    }

    public void setAB(int aB) {
        this.aB = aB;
    }

    public int getBa() {
        return Ba;
    }

    public void setBa(int ba) {
        Ba = ba;
    }

    public int getxB() {
        return xB;
    }

    public void setxB(int xB) {
        this.xB = xB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BeanA)) {
            return false;
        }
        BeanA beanA = (BeanA) o;
        return a == beanA.a && B == beanA.B && aa == beanA.aa && aB == beanA.aB && Ba == beanA.Ba && xB == beanA.xB;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, B, aa, aB, Ba, xB);
    }
}
