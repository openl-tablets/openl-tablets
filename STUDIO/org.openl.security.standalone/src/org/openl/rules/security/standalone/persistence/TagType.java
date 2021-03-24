package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "OpenL_Tag_Types")
public class TagType implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private boolean extensible;
    private boolean nullable;

    /**
     * Tag type name.
     */
    @Id
    @Column(name = "name", unique = true, nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isExtensible() {
        return extensible;
    }

    public void setExtensible(boolean extensible) {
        this.extensible = extensible;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TagType))
            return false;
        TagType tagType = (TagType) o;
        return getName().equals(tagType.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
