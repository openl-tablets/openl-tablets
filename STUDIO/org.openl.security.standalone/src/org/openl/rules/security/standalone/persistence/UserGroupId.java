package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserGroupId implements Serializable {
    private String loginName;
    private Long groupId;

    public UserGroupId() {
    }

    public UserGroupId(String loginName, Long groupId) {
        this.loginName = loginName;
        this.groupId = groupId;
    }

    @Column(name = "loginName", nullable = false, length = 50)
    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    @Column(name = "groupID", nullable = false)
    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserGroupId that)) {
            return false;
        }
        return Objects.equals(loginName, that.loginName) && Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loginName, groupId);
    }
}
