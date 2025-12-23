package org.openl.studio.security.pat.service;

import java.util.Collection;
import java.util.function.BiFunction;

import org.springframework.security.core.GrantedAuthority;

import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.User;
import org.openl.rules.webstudio.service.AdminUsers;
import org.openl.rules.webstudio.service.ExternalGroupService;
import org.openl.rules.webstudio.service.UserInfoUserDetailsServiceImpl;

/**
 * UserDetailsService implementation for Personal Access Token authentication.
 * <p>
 * Extends {@link UserInfoUserDetailsServiceImpl} to include external group privileges
 * when loading user details for PAT authentication. This ensures that users authenticated
 * via PAT have the same privileges as when authenticated via OAuth2/SAML.
 * </p>
 *
 * @since 6.0.0
 */
public class PatUserInfoUserDetailsServiceImpl extends UserInfoUserDetailsServiceImpl {

    private final ExternalGroupService externalGroupService;

    /**
     * Constructs a new PatUserInfoUserDetailsServiceImpl.
     *
     * @param userDao               the DAO for accessing user data
     * @param adminUsersInitializer the admin users initializer
     * @param privilegeMapper       the function for mapping user privileges
     * @param externalGroupService  the service for loading external groups
     */
    public PatUserInfoUserDetailsServiceImpl(UserDao userDao,
                                             AdminUsers adminUsersInitializer,
                                             BiFunction<String, Collection<? extends GrantedAuthority>, Collection<GrantedAuthority>> privilegeMapper,
                                             ExternalGroupService externalGroupService) {
        super(userDao, adminUsersInitializer, privilegeMapper);
        this.externalGroupService = externalGroupService;
    }

    /**
     * Maps user privileges including external groups.
     * <p>
     * This override loads external groups for the user and passes them to the parent
     * implementation to ensure PAT-authenticated users have the same privileges as
     * OAuth2/SAML-authenticated users.
     * </p>
     *
     * @param user the user entity
     * @param extraPrivileges additional privileges (ignored, external groups are loaded instead)
     * @return the complete set of granted authorities including external groups
     */
    @Override
    protected Collection<GrantedAuthority> mapPrivileges(User user, Collection<? extends GrantedAuthority> extraPrivileges) {
        var externalGroups = externalGroupService.findAllForUser(user.getLoginName());
        return super.mapPrivileges(user, externalGroups);
    }
}
