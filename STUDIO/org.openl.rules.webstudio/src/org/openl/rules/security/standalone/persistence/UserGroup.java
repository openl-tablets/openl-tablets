package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Mapping entity for the OpenL_User2Group join table.
 */
@Entity
@Table(name = "OpenL_User2Group")
public class UserGroup implements Serializable {
    @Getter(onMethod_ = {@EmbeddedId})
    @Setter
    private UserGroupId id;

    public UserGroup() {
    }

    public UserGroup(String loginName, Long groupId) {
        this.id = new UserGroupId(loginName, groupId);
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
