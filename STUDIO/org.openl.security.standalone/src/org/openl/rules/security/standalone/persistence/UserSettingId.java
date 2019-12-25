package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class UserSettingId implements Serializable {
    private String loginName;
    private String settingKey;

    public UserSettingId() {
    }

    public UserSettingId(String loginName, String settingKey) {
        this.loginName = loginName;
        this.settingKey = settingKey;
    }

    @Column(name = "loginName", nullable = false, length = 50)
    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    @Column(name = "settingKey", nullable = false, length = 64)
    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String key) {
        this.settingKey = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserSettingId that = (UserSettingId) o;
        return loginName.equals(that.loginName) && settingKey.equals(that.settingKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loginName, settingKey);
    }
}
