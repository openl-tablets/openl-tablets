package org.openl.studio.security.oauth2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiFunction;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Maps JWT claims to OpenL {@link GrantedAuthority}s.
 */
@RequiredArgsConstructor
public class OpenLJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter delegate;
    private final BiFunction<String, Collection<? extends GrantedAuthority>, Collection<GrantedAuthority>> privilegeMapper;

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        var username = jwt.getClaimAsString(JwtClaimNames.SUB);
        return new ArrayList<>(privilegeMapper.apply(username, delegate.convert(jwt)));
    }
}
