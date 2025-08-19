package org.openl.rules.security;

import org.springframework.security.core.GrantedAuthority;

public interface Privilege extends GrantedAuthority {

    String getName();

}
