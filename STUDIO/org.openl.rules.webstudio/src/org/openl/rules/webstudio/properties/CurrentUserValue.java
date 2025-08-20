package org.openl.rules.webstudio.properties;

import org.openl.studio.security.CurrentUserInfo;

/**
 * Handles information about currently loginned user.
 *
 * @author DLiauchuk
 */
public class CurrentUserValue implements ISystemValue {

    @Override
    public Object getValue() {
        CurrentUserInfo usInfo = new CurrentUserInfo();
        return usInfo.getUserName();
    }

}
