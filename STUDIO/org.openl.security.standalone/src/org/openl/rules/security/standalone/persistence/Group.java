package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
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
