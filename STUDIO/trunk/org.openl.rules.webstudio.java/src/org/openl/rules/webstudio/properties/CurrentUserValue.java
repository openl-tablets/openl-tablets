package org.openl.rules.webstudio.properties;

import org.acegisecurity.userdetails.UserDetails;
import org.openl.rules.webstudio.security.CurrentUserInfo;

/**
 * Handles information about currently loginned user.
 * 
 * @author DLiauchuk
 *
 */
public class CurrentUserValue implements ISystemValue {

    public String getValue() {
        CurrentUserInfo usInfo = new CurrentUserInfo();
        UserDetails usDet = usInfo.getUser();        
        return usDet.getUsername();
    }

}
