package org.openl.rules.webstudio.service;

import org.springframework.security.access.prepost.PreAuthorize;

public interface SecuredService {
    @PreAuthorize("hasAuthority('Developers')")
    void save(Foo foo);

    @PreAuthorize("hasPermission(#foo, 'VIEW')")
    void read(Foo foo);
}
