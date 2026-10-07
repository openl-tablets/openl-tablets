package org.openl.rules.webstudio.service;

import org.springframework.security.access.prepost.PreAuthorize;

public interface SecuredService {
    @PreAuthorize("hasAuthority('Developers')")
    void save(Foo foo);

    @PreAuthorize("hasPermission(#foo, 'READ')")
    void read(Foo foo);
}
