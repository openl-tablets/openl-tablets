package org.openl.rules.webstudio.service;

import java.util.function.Function;

import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.User;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * {@link org.springframework.security.core.userdetails.UserDetailsService} that can load UserInfo as UserDetails from
 * database.
 *
 * @author Andrey Naumenko
 * @author adjusted to new security model.
 */
public class UserInfoUserDetailsServiceImpl implements UserDetailsService {

    private final UserDao userDao;
    private final AdminUsers adminUsersInitializer;
    private final Function<SimpleUser, SimpleUser> authoritiesMapper;

    public UserInfoUserDetailsServiceImpl(UserDao userDao,
            AdminUsers adminUsersInitializer,
            Function<SimpleUser, SimpleUser> authoritiesMapper) {
        this.userDao = userDao;
        this.adminUsersInitializer = adminUsersInitializer;
        this.authoritiesMapper = authoritiesMapper;
    }

    @Override
    public org.openl.rules.security.User loadUserByUsername(String name) throws UsernameNotFoundException,
                                                                         DataAccessException {
        adminUsersInitializer.initIfSuperuser(name);
        User user = userDao.getUserByName(name);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("Unknown user: '%s'", name));
        }

        SimpleUser simpleUser = SimpleUser.builder()
            .setFirstName(user.getFirstName())
            .setLastName(user.getSurname())
            .setUsername(user.getLoginName())
            .setPasswordHash(user.getPasswordHash())
            .setEmail(user.getEmail())
            .setDisplayName(user.getDisplayName())
            .setExternalFlags(user.getUserExternalFlags())
            .build();
        return authoritiesMapper.apply(simpleUser);
    }
}
