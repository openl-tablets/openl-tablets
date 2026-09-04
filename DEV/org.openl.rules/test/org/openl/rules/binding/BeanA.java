package org.openl.rules.binding;

import lombok.Getter;
import lombok.Setter;

public class BeanA {
    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private BeanB[] beansB;

    @Override
    public int hashCode() {
        final var prime = 31;
        var result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        var other = (BeanA) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
}
