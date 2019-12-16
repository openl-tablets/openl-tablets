package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * This class contains user setting.
 */
@Entity
@Table(name = "OpenL_UserSettings")
public class UserSetting implements Serializable {
    private UserSettingId id;
    private String settingValue;

    @EmbeddedId
    public UserSettingId getId() {
        return id;
    }

    public void setId(UserSettingId id) {
        this.id = id;
    }

    @Column(name = "settingValue", nullable = false, length = 16)
    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String value) {
        this.settingValue = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserSetting that = (UserSetting) o;
        return Objects.equals(id, that.id) && Objects.equals(settingValue, that.settingValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, settingValue);
    }
}
