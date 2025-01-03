package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "OpenL_Tags")
public class Tag implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private TagType type;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "OpenL_Tags_ID_SEQ")
    @SequenceGenerator(sequenceName = "OpenL_Tags_ID_SEQ", name = "OpenL_Tags_ID_SEQ")
    @Column(name = "id")
    @Type(type = "java.lang.Long")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Tag value
     */
    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToOne(targetEntity = TagType.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "tag_type_id")
    public TagType getType() {
        return type;
    }

    public void setType(TagType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Tag))
            return false;
        Tag tag = (Tag) o;
        return Objects.equals(getId(), tag.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
