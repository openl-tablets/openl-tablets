package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * Group.
 *
 * @author Andrey Naumenko
 */
@Entity
@Table(name = "OpenL_Groups")
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String description;
    private Set<String> privileges;

    /**
     * Description of group.
     *
     * @return description
     */
    @Column(length = 200, name = "description")
    public String getDescription() {
        return description;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "OpenL_Groups_ID_SEQ")
    @SequenceGenerator(sequenceName = "OpenL_Groups_ID_SEQ", name = "OpenL_Groups_ID_SEQ")
    @Column(name = "id")
    @Type(type = "java.lang.Long")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Group name.
     *
     * @return
     */
    @Column(length = 65, name = "groupName", unique = true, nullable = false)
    public String getName() {
        return name;
    }

    /**
     * Comma separated list of user's privileges.
     *
     * @return
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "OpenL_Group_Authorities", joinColumns = @JoinColumn(name = "groupID"))
    @Column(length = 40, name = "authority", unique = true, nullable = false)
    public Set<String> getPrivileges() {
        return privileges;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrivileges(Set<String> privileges) {
        this.privileges = privileges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Group group = (Group) o;

        return id != null ? id.equals(group.id) : group.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Group{id=" + id + '}';
    }
}
