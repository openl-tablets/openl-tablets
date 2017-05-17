package org.openl.rules.webstudio.security;

import java.util.*;

import org.openl.rules.security.DefaultPrivileges;
import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.service.PrivilegesEvaluator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

public class ActiveDirectoryAuthoritiesMapper implements GrantedAuthoritiesMapper {
    private final GroupDao groupDao;

    public ActiveDirectoryAuthoritiesMapper(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
        for (GrantedAuthority authority : authorities) {
            Group group = groupDao.getGroupByName(authority.getAuthority());
            if (group != null) {
                grantedAuthorities.add(new SimpleGroup(group.getName(), group.getDescription(),
                        PrivilegesEvaluator.createPrivileges(group)));
            } else {
                // It's not an OpenL group. Keep it as is.
                grantedAuthorities.add(authority);
            }
        }
        return grantedAuthorities;
    }
}
