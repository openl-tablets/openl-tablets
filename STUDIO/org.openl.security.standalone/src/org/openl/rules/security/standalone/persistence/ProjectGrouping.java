package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;

import org.openl.util.StringUtils;

public class ProjectGrouping implements Serializable {
    private static final long serialVersionUID = 1L;
    private String loginName;
    private String group1;
    private String group2;
    private String group3;

    /**
     * Login name of user.
     */
    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getGroup1() {
        return group1;
    }

    public void setGroup1(String group1) {
        this.group1 = StringUtils.trimToEmpty(group1);
    }

    public String getGroup2() {
        return group2;
    }

    public void setGroup2(String group2) {
        this.group2 = StringUtils.trimToEmpty(group2);
    }

    public String getGroup3() {
        return group3;
    }

    public void setGroup3(String group3) {
        this.group3 = StringUtils.trimToEmpty(group3);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ProjectGrouping user = (ProjectGrouping) o;

        return Objects.equals(loginName, user.loginName);
    }

    @Override
    public int hashCode() {
        return loginName != null ? loginName.hashCode() : 0;
    }

}
