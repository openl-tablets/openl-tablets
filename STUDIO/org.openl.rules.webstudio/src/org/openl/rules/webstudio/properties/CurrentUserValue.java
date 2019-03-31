package org.openl.rules.webstudio.properties;

import org.openl.rules.webstudio.security.CurrentUserInfo;

/**
 * Handles information about currently loginned user.
 * 
 * @author DLiauchuk
 *
 */
public class CurrentUserValue implements ISystemValue {

    public Object getValue() {
        CurrentUserInfo usInfo = new CurrentUserInfo();
        return usInfo.getUserName();
    }

}
