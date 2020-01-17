package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/**
 * This class contains information about application user.
 *
 * @author Andrey Naumenko
 */
@Entity
@Table(name = "OpenL_Users") // "USER" is a reserved word in SQL92/SQL99
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String loginName;
    private String passwordHash;
    private Set<Group> groups;
    private String firstName;
    private String surname;

    /**
     * First name.
     */
    @Column(name = "firstName", length = 50)
    public String getFirstName() {
        return firstName;
    }

    /**
     * User's groups.
     */
    @ManyToMany(targetEntity = Group.class, fetch = FetchType.EAGER, cascade = javax.persistence.CascadeType.MERGE)
    @JoinTable(name = "OpenL_User2Group", joinColumns = { @JoinColumn(name = "loginName") }, inverseJoinColumns = {
            @JoinColumn(name = "groupID") })
    public Set<Group> getGroups() {
        return groups;
    }

    /**
     * Login name of user.
     */
    @Id
    @Column(name = "loginName", length = 50, nullable = false, unique = true)
    public String getLoginName() {
        return loginName;
    }

    /**
     * Password of user.
     */
    @Column(name = "password", length = 128, nullable = true)
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Surname.
     */
    @Column(name = "surname", length = 50)
    public String getSurname() {
        return surname;
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

    public void setSurname(String surname) {
        this.surname = surname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        User user = (User) o;

        return loginName != null ? loginName.equals(user.loginName) : user.loginName == null;
    }

    @Override
    public int hashCode() {
        return loginName != null ? loginName.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User{loginName=" + loginName + '}';
    }
}
