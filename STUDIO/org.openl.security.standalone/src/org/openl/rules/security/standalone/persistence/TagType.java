package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "OpenL_Tag_Types")
public class TagType implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private boolean extensible;
    private boolean nullable;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "OpenL_Tag_Types_ID_SEQ")
    @SequenceGenerator(sequenceName = "OpenL_Tag_Types_ID_SEQ", name = "OpenL_Tag_Types_ID_SEQ")
    @Column(name = "id")
    @Type(type = "java.lang.Long")
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TagType))
            return false;
        TagType tagType = (TagType) o;
        return Objects.equals(getId(), tagType.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
