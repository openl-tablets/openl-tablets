package org.openl.rules.security.standalone.authentication;

import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.User;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author nsamatov.
 */
public class RehashingDaoAuthenticationProvider extends DaoAuthenticationProvider {
    private final OpenLPasswordEncoder passwordEncoder;
    private final UserDao userDao;

    public RehashingDaoAuthenticationProvider(OpenLPasswordEncoder passwordEncoder, UserDao userDao) {
        this.passwordEncoder = passwordEncoder;
        this.userDao = userDao;
        setPasswordEncoder(passwordEncoder);
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        if (userDetails instanceof org.openl.rules.security.User) {
            org.openl.rules.security.User user = (org.openl.rules.security.User) userDetails;
            if (!user.isInternalUser()) {
                throw new BadCredentialsException("Only internal users can be authenticated using RehashingDaoAuthenticationProvider");
            }
        }
        super.additionalAuthenticationChecks(userDetails, authentication);

        String oldHashedPassword = userDetails.getPassword();
        if (passwordEncoder.rehashIsNeeded(oldHashedPassword)) {
            String rehashedPassword = passwordEncoder.encode(authentication.getCredentials().toString());

            User user = userDao.getUserByName(userDetails.getUsername());
            user.setPasswordHash(rehashedPassword);

            userDao.update(user);
        }
    }
}
