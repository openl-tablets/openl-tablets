package org.openl.rules.webstudio.properties;

import org.openl.rules.webstudio.security.CurrentUserInfo;

import org.springframework.security.core.userdetails.UserDetails;

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
