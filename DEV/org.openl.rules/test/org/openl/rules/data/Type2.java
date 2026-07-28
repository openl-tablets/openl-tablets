package org.openl.rules.data;

import lombok.Getter;
import lombok.Setter;

public class Type2 {

    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private Type1[] types;

    public Type2() {
        super();
    }

    public Type2(String name, Type1[] types) {
        super();
        this.name = name;
        this.types = types;
    }

}
