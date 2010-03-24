package org.openl.rules.security.standalone.persistence;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Group.
 *
 * @author Andrey Naumenko
 */
@Entity
@Table(name = "UserGroup")
public class Group extends PersistentObject {
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    private String privileges;
    private Set<Group> includedGroups;
    private Set<AccessControlEntry> accessControlEntries;

    /**
     * Group's access control entries.
     *
     * @return
     */
    @OneToMany(targetEntity = AccessControlEntry.class, mappedBy = "group")
    @Cascade(value = { CascadeType.ALL, CascadeType.DELETE_ORPHAN })
    public Set<AccessControlEntry> getAccessControlEntries() {
        return accessControlEntries;
    }

    /**
     * Description of group.
     *
     * @return description
     */
    @Column(length = 100, name = "Description")
    public String getDescription() {
        return description;
    }

    @Override
    @Id
    @GeneratedValue(generator = "nativeId")
    @GenericGenerator(name = "nativeId", strategy = "native")
    @Column(name = "GroupID")
    @Type(type = "java.lang.Long")
    public Long getId() {
        return super.getId();
    }

    /**
     * Included groups.
     *
     * @return
     */
    @ManyToMany(targetEntity = Group.class, fetch = FetchType.LAZY)
    @JoinTable(name = "Group2Group", joinColumns = { @JoinColumn(name = "GroupID") }, inverseJoinColumns = { @JoinColumn(name = "IncludedGroupID") })
    @Cascade(value = { CascadeType.MERGE, CascadeType.SAVE_UPDATE })
    public Set<Group> getIncludedGroups() {
        return includedGroups;
    }

    /**
     * Group name.
     *
     * @return
     */
    @Column(length = 40, name = "GroupName", unique = true, nullable = false)
    public String getName() {
        return name;
    }

    /**
     * Comma separated list of user's privileges.
     *
     * @return
     */
    @Column(name = "Privileges", length = 200)
    public String getPrivileges() {
        return privileges;
    }

    public void setAccessControlEntries(Set<AccessControlEntry> accessControlEntries) {
        this.accessControlEntries = accessControlEntries;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIncludedGroups(Set<Group> includedGroups) {
        this.includedGroups = includedGroups;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrivileges(String privileges) {
        this.privileges = privileges;
    }
}
