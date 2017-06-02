package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * Group.
 *
 * @author Andrey Naumenko
 */
@Entity
@Table(name = "OpenLGroups")
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String description;
    private String privileges;
    private Set<Group> includedGroups;
    private Set<User> users;
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
    @GeneratedValue(
        strategy=GenerationType.AUTO)
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
    @JoinTable(name = "OpenLGroup2Group", joinColumns = { @JoinColumn(name = "groupID") }, inverseJoinColumns = { @JoinColumn(name = "includedGroupID") })
    public Set<Group> getIncludedGroups() {
        return includedGroups;
    }

    /**
     * Parent groups.
     *
     * @return
     */
    @ManyToMany(targetEntity = Group.class, fetch = FetchType.LAZY, cascade = javax.persistence.CascadeType.MERGE)
    @JoinTable(name = "OpenLGroup2Group", joinColumns = { @JoinColumn(name = "includedGroupID") }, inverseJoinColumns = { @JoinColumn(name = "groupID") })
    public Set<Group> getParentGroups() {
        return parentGroups;
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
    @Column(name = "userPrivileges", length = 1000) // Privileges is reserved word for Oracle Data base
    public String getPrivileges() {
        return privileges;
    }

    /**
     * Users belonging to this group. Users count can be too big - we should use
     * lazy loading here
     * 
     * @return belonging to this group
     */
    @ManyToMany(targetEntity = User.class, fetch = FetchType.LAZY, cascade = javax.persistence.CascadeType.MERGE)
    @JoinTable(name = "OpenLUser2Group", joinColumns = { @JoinColumn(name = "groupID") }, inverseJoinColumns = { @JoinColumn(name = "loginName") })
    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
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

    public void setPrivileges(String privileges) {
        this.privileges = privileges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

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
