package org.openl.rules.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

public class RoleHierarchyImpl implements RoleHierarchy {

    @Override
    public Collection<? extends GrantedAuthority> getReachableGrantedAuthorities(
            Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return AuthorityUtils.NO_AUTHORITIES;
        }
        Collection<GrantedAuthority> reachableGrantedAuthorities = new HashSet<>();
        Queue<GrantedAuthority> queue = new LinkedList<>(authorities);
        Set<String> p = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        while (!queue.isEmpty()) {
            GrantedAuthority ga = queue.poll();
            if (ga instanceof Group) {
                Group group = (Group) ga;
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
