package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.*;

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
    private Set<Group> includedGroups;
    private Set<Group> parentGroups;

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
     * Included groups.
     *
     * @return
     */
    @ManyToMany(targetEntity = Group.class, fetch = FetchType.EAGER, cascade = javax.persistence.CascadeType.MERGE)
    @JoinTable(name = "OpenL_Group2Group", joinColumns = { @JoinColumn(name = "groupID") }, inverseJoinColumns = {
            @JoinColumn(name = "includedGroupID") })
    public Set<Group> getIncludedGroups() {
        return includedGroups;
    }

    /**
     * Group name.
     *
     * @return
     */
    @Column(length = 40, name = "groupName", unique = true, nullable = false)
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

    public void setIncludedGroups(Set<Group> includedGroups) {
        this.includedGroups = includedGroups;
    }

    public void setParentGroups(Set<Group> parentGroups) {
        this.parentGroups = parentGroups;
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
