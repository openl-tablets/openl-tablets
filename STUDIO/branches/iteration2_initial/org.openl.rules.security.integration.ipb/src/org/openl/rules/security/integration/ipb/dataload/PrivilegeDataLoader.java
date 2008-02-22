package org.openl.rules.security.integration.ipb.dataload;

import com.exigen.epb.security.model.Authority;
import com.exigen.epb.security.model.Privilege;
import com.exigen.epb.security.services.AccessManagementService;
import com.exigen.ipb.base.dataload.DataLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.openl.rules.security.Privileges;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aliaksandr Antonik.
 */
public class PrivilegeDataLoader implements DataLoader {
    private AccessManagementService accessManagemenetService;
    private static final Log log = LogFactory.getLog(PrivilegeDataLoader.class);
    private static final String OWNER = "USER";

    @Transactional
    public void load() {
        List<Authority> authorities = new ArrayList<Authority>();
        for (String priv : enumPrivileges(Privileges.class)) {
            Privilege privilege = new Privilege();
            privilege.setName(priv);
            privilege.setOwner(OWNER);
            authorities.add(privilege);
        }

        log.info("loading privileges. " + authorities.size() + " items.");
        accessManagemenetService.deployAuthorities(authorities, true);
    }

    public void setAccessManagemenetService(AccessManagementService accessManagemenetService) {
        this.accessManagemenetService = accessManagemenetService;
    }

    protected static List<String> enumPrivileges(Class<?> clazz) {
        List<String> ret = new ArrayList<String>();
        final String PREFIX = "PRIVILEGE_";

        for (Field field : clazz.getFields()) {
            int mod = field.getModifiers();
            if (field.getType().equals(String.class) &&
                Modifier.isPublic(mod) && Modifier.isStatic(mod) &&
                Modifier.isFinal(mod) && field.getName().startsWith(PREFIX)) {
                try {
                    String privName = (String) field.get(null);
                    if (!StringUtils.isBlank(privName))
                        ret.add(privName);
                } catch (IllegalAccessException e) {
                    log.warn("exception while inspecting " + clazz, e);
                }
            }
        }

        return ret;
    }
}
