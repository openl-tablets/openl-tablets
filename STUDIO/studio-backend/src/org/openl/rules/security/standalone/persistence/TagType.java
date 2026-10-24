package org.openl.rules.security.standalone.persistence;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "OpenL_Tag_Types")
public class TagType implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private boolean extensible;
    private boolean nullable;
    private @Nullable Integer displayOrder;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Tag type name.
     */
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

    /**
     * Where the tag type is listed among the others, starting from 0. A tag type without one is listed after
     * the ordered ones, by name.
     */
    @Column(name = "display_order")
    public @Nullable Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(@Nullable Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TagType tagType))
            return false;
        return Objects.equals(getId(), tagType.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
