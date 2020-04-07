package org.openl.generated.epbds6830;

import java.util.Objects;

public class BeanB extends BeanA {
    private String txt = "msg";

    public BeanB() {
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
