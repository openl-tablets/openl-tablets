package org.openl.rules.webstudio.service;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.security.standalone.dao.UserDao;

/**
 * {@link UserDetailsService} that can load UserInfo as UserDetails from database.
 *
 * @author Andrey Naumenko
 * @author adjusted to new security model.
 */
public class UserInfoUserDetailsServiceImpl implements UserDetailsService {

    private final UserDao userDao;
    private final AdminUsers adminUsersInitializer;
    private final BiFunction<String, Collection<? extends GrantedAuthority>, Collection<GrantedAuthority>> privilegeMapper;

    public UserInfoUserDetailsServiceImpl(UserDao userDao,
                                          AdminUsers adminUsersInitializer,
                                          BiFunction<String, Collection<? extends GrantedAuthority>, Collection<GrantedAuthority>> privilegeMapper) {
        this.userDao = userDao;
        this.adminUsersInitializer = adminUsersInitializer;
        this.privilegeMapper = privilegeMapper;
    }

    @Override
    public User loadUserByUsername(String name) throws UsernameNotFoundException, DataAccessException {

        adminUsersInitializer.initIfSuperuser(name);

        var user = userDao.getUserByName(name);
        if (user == null) {
            throw new UsernameNotFoundException("Unknown user: '%s'".formatted(name));
        }

        var privileges = mapPrivileges(user, List.of());

        var simpleUser = SimpleUser.builder()
                .setFirstName(user.getFirstName())
                .setLastName(user.getSurname())
                .setUsername(user.getLoginName())
                .setPrivileges(privileges)
                .setPasswordHash(user.getPasswordHash())
                .setEmail(user.getEmail())
                .setDisplayName(user.getDisplayName())
                .setExternalFlags(user.getUserExternalFlags())
                .build();

        return simpleUser;
    }

    protected Collection<GrantedAuthority> mapPrivileges(org.openl.rules.security.standalone.persistence.User user,
                                                         Collection<? extends GrantedAuthority> extraPrivileges) {
        return privilegeMapper.apply(user.getLoginName(), extraPrivileges);
    }
}
