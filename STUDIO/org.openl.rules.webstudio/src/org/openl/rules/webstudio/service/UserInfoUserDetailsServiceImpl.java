package org.openl.rules.webstudio.service;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.security.standalone.dao.UserDao;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * {@link UserDetailsService} that can load UserInfo as UserDetails from database.
 *
 * @author Andrey Naumenko
 * @author adjusted to new security model.
 */
public class UserInfoUserDetailsServiceImpl implements UserDetailsService {

    private final UserDao userDao;
    private final AdminUsers adminUsersInitializer;
    private final BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper;

    public UserInfoUserDetailsServiceImpl(UserDao userDao,
            AdminUsers adminUsersInitializer,
            BiFunction<String, Collection<? extends GrantedAuthority>, Collection<Privilege>> privilegeMapper) {
        this.userDao = userDao;
        this.adminUsersInitializer = adminUsersInitializer;
        this.privilegeMapper = privilegeMapper;
    }

    @Override
    public User loadUserByUsername(String name) throws UsernameNotFoundException, DataAccessException {

        adminUsersInitializer.initIfSuperuser(name);

        org.openl.rules.security.standalone.persistence.User user = userDao.getUserByName(name);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("Unknown user: '%s'", name));
        }

        Collection<Privilege> privileges = privilegeMapper.apply(user.getLoginName(), Collections.emptyList());

        SimpleUser simpleUser = SimpleUser.builder()
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
}
