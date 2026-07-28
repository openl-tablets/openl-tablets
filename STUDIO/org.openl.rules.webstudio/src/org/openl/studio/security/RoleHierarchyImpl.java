package org.openl.studio.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.stream.Collectors;

import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import org.openl.rules.security.Group;

public class RoleHierarchyImpl implements RoleHierarchy {

    @Override
    public Collection<? extends GrantedAuthority> getReachableGrantedAuthorities(
            Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return AuthorityUtils.NO_AUTHORITIES;
        }
        var reachableGrantedAuthorities = new HashSet<GrantedAuthority>();
        var queue = new LinkedList<GrantedAuthority>(authorities);
        var p = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        while (!queue.isEmpty()) {
            var ga = queue.poll();
            if (ga instanceof Group group) {
                if (group.getPrivileges() != null) {
                    for (GrantedAuthority g : group.getPrivileges()) {
                        if (!p.contains(g.getAuthority())) {
                            queue.add(g);
                            p.add(g.getAuthority());
                        }
                    }
                }
            }
            reachableGrantedAuthorities.add(ga);
        }
        return reachableGrantedAuthorities;
    }
}
