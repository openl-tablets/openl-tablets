package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@Table(name = "OpenL_External_Groups")
@IdClass(ExternalGroup.PK.class)
public class ExternalGroup implements Serializable {

    private static final long serialVersionUID = 5117085519399896506L;

    @Id
    @Column(name = "groupName", length = 50)
    private String groupName;

    @Id
    @Column(name = "loginName", length = 50)
    private String loginName;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public static class PK implements Serializable {
        private String groupName;
        private String loginName;

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public String getLoginName() {
            return loginName;
        }

        public void setLoginName(String loginName) {
            this.loginName = loginName;
        }
    }
}
