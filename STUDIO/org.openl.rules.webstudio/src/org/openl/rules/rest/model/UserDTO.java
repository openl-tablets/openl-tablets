package org.openl.rules.rest.model;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.rules.security.Privilege;

public class UserDTO {

    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Set<String> groups;
    private boolean currentUser;
    private boolean superUser;
    private boolean internalUser;
    private boolean unsafePassword;

    public UserDTO() {
    }

    public UserDTO(String firstName,
            String lastName,
            String username,
            String email,
            String password,
            Collection<Privilege> authorities) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.email = email;
        this.password = password;
        this.groups = authorities.stream().map(Privilege::getName).collect(Collectors.toSet());
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public boolean isSuperUser() {
        return superUser;
    }

    public void setSuperUser(boolean superUser) {
        this.superUser = superUser;
    }

    public boolean isCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(boolean currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isInternalUser() {
        return internalUser;
    }

    public void setInternalUser(boolean internalUser) {
        this.internalUser = internalUser;
    }

    public boolean isUnsafePassword() {
        return unsafePassword;
    }

    public void setUnsafePassword(boolean unsafePassword) {
        this.unsafePassword = unsafePassword;
    }
}
