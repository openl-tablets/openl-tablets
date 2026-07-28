package org.openl.rules.data;

import lombok.Getter;
import lombok.Setter;

public class Type1 {

    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String value;

    public Type1() {
        super();
    }

    public Type1(String name, String value) {
        super();
        this.name = name;
        this.value = value;
    }

}
