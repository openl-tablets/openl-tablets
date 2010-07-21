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

/**
 * This class contains information about application user.
 *
 * @author Andrey Naumenko
 */
@Entity
public class User extends PersistentObject {
    private static final long serialVersionUID = 1L;
    private String loginName;
    private String passwordHash;
    private String privileges;
    private Set<Group> groups;
    private String firstName;
    private String surname;
    private Set<AccessControlEntry> accessControlEntries;

    /**
     * User's access control entries.
     *
     * @return
     */
    @OneToMany(targetEntity = AccessControlEntry.class, mappedBy = "user")
    @Cascade(value = { CascadeType.ALL, CascadeType.DELETE_ORPHAN })
    public Set<AccessControlEntry> getAccessControlEntries() {
        return accessControlEntries;
    }

    /**
     * First name.
     *
     * @return
     */
    @Column(name = "FirstName", length = 50)
    public String getFirstName() {
        return firstName;
    }

    /**
     * User's groups.
     *
     * @return
     */
    @ManyToMany(targetEntity = Group.class, fetch = FetchType.LAZY)
    @JoinTable(name = "User2Group", joinColumns = { @JoinColumn(name = "UserID") }, inverseJoinColumns = { @JoinColumn(name = "GroupID") })
    @Cascade(value = { CascadeType.MERGE, CascadeType.SAVE_UPDATE })
    public Set<Group> getGroups() {
        return groups;
    }

    @Override
    @Id
    @GeneratedValue(generator = "nativeId")
    @GenericGenerator(name = "nativeId", strategy = "native")
    @Column(name = "UserID")
    @Type(type = "java.lang.Long")
    public Long getId() {
        return super.getId();
    }

    /**
     * Login name of user.
     *
     * @return
     */
    @Column(name = "LoginName", length = 50, nullable = false, unique = true)
    public String getLoginName() {
        return loginName;
    }

    /**
     * Password of user.
     *
     * @return
     */
    @Column(name = "Password", length = 32, nullable = false)
    public String getPasswordHash() {
        return passwordHash;
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

    /**
     * Surname.
     *
     * @return
     */
    @Column(name = "Surname", length = 50)
    public String getSurname() {
        return surname;
    }

    public void setAccessControlEntries(Set<AccessControlEntry> accessControlEntries) {
        this.accessControlEntries = accessControlEntries;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setPrivileges(String privileges) {
        this.privileges = privileges;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }
}
