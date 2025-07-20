package org.openl.generated.epbds7084;

import java.util.Objects;

public class BeanA {
    protected int a = 1;
    protected int B = 2;
    protected int aa = 4;
    protected int aB = 8;
    protected int Ba = 16;
    protected int BB = 32;
    protected int xB = 64;

    public BeanA() {
    }

    public BeanA(int a, int b, int aa, int aB, int ba, int BB, int xB) {
        this.a = a;
        B = b;
        this.aa = aa;
        this.aB = aB;
        Ba = ba;
        this.BB = BB;
        this.xB = xB;
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
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

    public int getAB() {
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

    public int getBB() {
        return BB;
    }

    public void setBB(int BB) {
        this.BB = BB;
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
        return a == beanA.a && B == beanA.B && aa == beanA.aa && aB == beanA.aB && Ba == beanA.Ba && BB == beanA.BB && xB == beanA.xB;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, B, aa, aB, Ba, BB, xB);
    }
}
