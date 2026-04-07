package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Mapping entity for the OpenL_User2Group join table.
 */
@Entity
@Table(name = "OpenL_User2Group")
public class UserGroup implements Serializable {
    private UserGroupId id;

    public UserGroup() {
    }

    public UserGroup(String loginName, Long groupId) {
        this.id = new UserGroupId(loginName, groupId);
    }

    @EmbeddedId
    public UserGroupId getId() {
        return id;
    }

    public void setId(UserGroupId id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserGroup that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
