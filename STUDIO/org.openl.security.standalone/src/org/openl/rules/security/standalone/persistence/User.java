package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.openl.rules.security.UserExternalFlags;

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
    private String email;
    private String displayName;
    private int flags;

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
    @ManyToMany(targetEntity = Group.class, fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "OpenL_User2Group", joinColumns = {@JoinColumn(name = "loginName")}, inverseJoinColumns = {
            @JoinColumn(name = "groupID")})
    public Set<Group> getGroups() {
        return groups;
    }

    /**
     * Login name of user.
     */
    @Id
    @Column(name = "loginName", length = 50, nullable = false, unique = true)
    @UsernameConstraints
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

    @Column(name = "email", length = 254)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Column(name = "displayName", length = 64)
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Column(name = "flags")
    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    @Transient
    public UserExternalFlags getUserExternalFlags() {
        return UserExternalFlags.builder(flags).build();
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
