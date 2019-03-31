package org.openl.rules.webstudio.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.service.PrivilegesEvaluator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

public class OpenLGroupsAuthoritiesMapper implements GrantedAuthoritiesMapper {
    private final GroupDao groupDao;

    public OpenLGroupsAuthoritiesMapper(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        for (GrantedAuthority authority : authorities) {
            Group group = groupDao.getGroupByName(authority.getAuthority());
            if (group != null) {
                grantedAuthorities.add(PrivilegesEvaluator.wrap(group));
            } else {
                // It's not an OpenL group. Keep it as is.
                grantedAuthorities.add(authority);
            }
        }
        return grantedAuthorities;
    }
}
