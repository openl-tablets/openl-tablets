package org.openl.rules.testmethod.export;

import lombok.Getter;
import lombok.Setter;

public class A {
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private Integer[] values;

    public A(String name, Integer... values) {
        this.name = name;
        this.values = values;
    }
}
