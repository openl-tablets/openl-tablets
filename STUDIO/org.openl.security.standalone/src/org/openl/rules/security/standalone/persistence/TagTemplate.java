package org.openl.rules.security.standalone.persistence;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "OpenL_Tag_Templates")
public class TagTemplate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Getter(onMethod_ = {@Id})
    @Setter
    private String template;
    @Getter
    @Setter
    private int priority;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TagTemplate that))
            return false;
        return getTemplate().equals(that.getTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTemplate());
    }
}
