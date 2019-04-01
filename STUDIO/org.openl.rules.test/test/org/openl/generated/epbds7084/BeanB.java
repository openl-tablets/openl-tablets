package org.openl.generated.epbds7084;

import java.util.Objects;

public class BeanB extends BeanA {
    String txt = "MyTXT";

    public BeanB() {
    }

    public BeanB(int a, int b, int aa, int aB, int ba, int BB, int xB, String txt) {
        super(a, b, aa, aB, ba, BB, xB);
        this.txt = txt;
    }

    public String getTxt() {
        return txt;
    }

    public void setTxt(String txt) {
        this.txt = txt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BeanB)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        BeanB beanB = (BeanB) o;
        return Objects.equals(txt, beanB.txt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), txt);
    }
}
