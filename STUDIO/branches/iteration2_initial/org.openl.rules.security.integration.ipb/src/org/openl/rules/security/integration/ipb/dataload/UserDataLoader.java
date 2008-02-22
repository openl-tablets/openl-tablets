package org.openl.rules.security.integration.ipb.dataload;

import com.exigen.epb.security.model.Privilege;
import com.exigen.epb.security.model.User;
import com.exigen.epb.security.services.AccessManagementService;
import com.exigen.epb.security.services.UserManagementService;
import com.exigen.ipb.base.dataload.DataLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aliaksandr Antonik.
 */
public class UserDataLoader implements DataLoader {
    private static final Log log = LogFactory.getLog(UserDataLoader.class);
    private UserManagementService userManagementService;
    private AccessManagementService accessManagemenetService;
    private List<UserPrivilegeInfo> usersInfo;

    @Transactional
    public void load() {
        log.info("loading users. " + usersInfo.size() + " items.");

        Map<String, Privilege> privilegeMap = getPrivilegeMap();
        for (UserPrivilegeInfo userInfo : usersInfo) {
            userManagementService.savePrincipal(userInfo.getUser());
            grant(userInfo, privilegeMap);
        }
    }

    private void grant(UserPrivilegeInfo userInfo, Map<String, Privilege> privilegeMap) {
        if (userInfo.getPrivileges() != null) {
            User user = userInfo.getUser();

            for (String privilegeName : userInfo.getPrivileges()) {
                Privilege privilege = privilegeMap.get(privilegeName);
                if (privilege != null) {
                    accessManagemenetService.grantAuthority(user, privilege);
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("privilege '" + privilegeName + "' does not exist. User: " + user.getName());
                    }
                }
            }
        }
    }

    private Map<String, Privilege> getPrivilegeMap() {
        Map<String, Privilege> privilegeMap = new HashMap<String, Privilege>();
        for (Privilege p : accessManagemenetService.findAllPrivileges()) {
            privilegeMap.put(p.getName(), p);
        }
        return privilegeMap;
    }

    public void setUsersInfo(List<UserPrivilegeInfo> usersInfo) {
        this.usersInfo = usersInfo;
    }

    public void setUserManagementService(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    public void setAccessManagemenetService(AccessManagementService accessManagemenetService) {
        this.accessManagemenetService = accessManagemenetService;
    }
}
