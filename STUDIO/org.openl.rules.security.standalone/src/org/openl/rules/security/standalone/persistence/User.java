package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * This class contains information about application user.
 *
 * @author Andrey Naumenko
 */
@Entity
@Table(name="OpenLUser") // "USER" is a reserved word in SQL92/SQL99 
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String loginName;
    private String passwordHash;
    private String privileges;
    private Set<Group> groups;
    private String firstName;
    private String surname;
    private String origin;

    /**
     * First name.
     */
    @Column(name = "FirstName", length = 50)
    public String getFirstName() {
        return firstName;
    }

    /**
     * User's groups.
     */
    @ManyToMany(targetEntity = Group.class, fetch = FetchType.EAGER, cascade = javax.persistence.CascadeType.MERGE)
    @JoinTable(name = "User2Group", joinColumns = { @JoinColumn(name = "UserID") }, inverseJoinColumns = { @JoinColumn(name = "GroupID") })
    public Set<Group> getGroups() {
        return groups;
    }

    @Id
    @GeneratedValue
    @Column(name = "UserID")
    @Type(type = "java.lang.Long")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Login name of user.
     */
    @Column(name = "LoginName", length = 50, nullable = false, unique = true)
    public String getLoginName() {
        return loginName;
    }

    /**
     * Password of user.
     */
    @Column(name = "Password", length = 128, nullable = false)
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Comma separated list of user's privileges.
     */
    @Column(name = "UserPrivileges", length = 1000) // Privileges is reserved word for Oracle Data base
    public String getPrivileges() {
        return privileges;
    }

    /**
     * Surname.
     */
    @Column(name = "Surname", length = 50)
    public String getSurname() {
        return surname;
    }

    @Column(name = "origin", length = 100)
    public String getOrigin() {
        return origin;
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

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return id != null ? id.equals(user.id) : user.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User{id=" + id + '}';
    }
}
