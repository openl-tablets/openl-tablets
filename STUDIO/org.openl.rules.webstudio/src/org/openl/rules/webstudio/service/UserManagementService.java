package org.openl.rules.webstudio.service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.UserExternalFlags;
import org.openl.rules.security.UserExternalFlags.Feature;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.persistence.User;
import org.openl.security.acl.JdbcMutableAclService;
import org.openl.util.StringUtils;

/**
 * @author Andrei Astrouski
 */
public class UserManagementService {

    private final UserDao userDao;
    private final GroupDao groupDao;
    private final SessionRegistry sessionRegistry;
    private final PasswordEncoder passwordEncoder;
    private final JdbcMutableAclService aclService;

    public UserManagementService(UserDao userDao,
                                 GroupDao groupDao,
                                 SessionRegistry sessionRegistry,
                                 PasswordEncoder passwordEncoder,
                                 JdbcMutableAclService aclService) {
        this.userDao = userDao;
        this.groupDao = groupDao;
        this.sessionRegistry = sessionRegistry;
        this.passwordEncoder = passwordEncoder;
        this.aclService = aclService;
    }

    @Transactional(readOnly = true)
    public List<org.openl.rules.security.User> getAllUsers() {
        return userDao.getAllUsers().stream().map(this::createSecurityUser).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> findUserNames(String searchTerm, int limit) {
        return userDao.findUserNames(searchTerm, limit);
    }

    @Transactional(readOnly = true)
    public org.openl.rules.security.User getUser(String username) {
        return Optional.ofNullable(userDao.getUserByName(username)).map(this::createSecurityUser).orElse(null);
    }

    /**
     * Returns the users belonging to the group. Both directly assigned users and users having
     * a matched external group with the same name are included.
     *
     * <p>The group privileges are not resolved for the returned users.
     *
     * @param groupName group name
     * @return users of the group ordered by login name
     */
    @Transactional(readOnly = true)
    public List<org.openl.rules.security.User> getUsersInGroup(String groupName) {
        return userDao.getUsersInGroup(groupName)
                .stream()
                .map(user -> createSecurityUser(user, Collections.emptySet()))
                .collect(Collectors.toList());
    }

    /**
     * Counts the users belonging to the group. A user assigned both directly and through a matched
     * external group is counted once.
     *
     * @param groupName group name
     * @return number of distinct users of the group
     */
    @Transactional(readOnly = true)
    public long countUsersInGroup(String groupName) {
        return userDao.countUsersInGroup(groupName);
    }

    @Transactional
    public boolean existsByName(String name) {
        return userDao.existsByName(name);
    }

    public void addUser(String user,
                        String firstName,
                        String lastName,
                        String password,
                        String email,
                        String displayName) {
        var persistUser = new User();
        persistUser.setLoginName(user);
        persistUser.setPasswordHash(StringUtils.isNotBlank(password) ? passwordEncoder.encode(password) : null);
        persistUser.setFirstName(firstName);
        persistUser.setSurname(lastName);
        persistUser.setEmail(email);
        persistUser.setDisplayName(displayName);

        userDao.save(persistUser);
    }

    /**
     * Update user info in Db by external data from the 3rd party identity providers (AD, SAML, CAS, OAuth2 etc.)
     */
    public void syncUserData(String user, String firstName, String lastName, String email, String displayName) {

        // Get
        var persistUser = userDao.getUserByName(user);
        var isNewUser = persistUser == null;
        if (isNewUser) {
            persistUser = new User();
            persistUser.setLoginName(user);
        }
        var flags = UserExternalFlags.builder()
                .applyFeature(Feature.EXTERNAL_FIRST_NAME, StringUtils.isNotBlank(firstName))
                .applyFeature(Feature.EXTERNAL_LAST_NAME, StringUtils.isNotBlank(lastName))
                .applyFeature(Feature.EXTERNAL_EMAIL, StringUtils.isNotBlank(email))
                .applyFeature(Feature.EMAIL_VERIFIED,
                        StringUtils.isNotBlank(email) || persistUser.getUserExternalFlags().isEmailVerified())
                .applyFeature(Feature.EXTERNAL_DISPLAY_NAME, StringUtils.isNotBlank(displayName))
                .build();

        if (!flags.isDisplayNameExternal() && !isNewUser) {
            displayName = persistUser.getDisplayName();

            // try to restore display name from previous pattern
            String prevFirstName = StringUtils.trimToEmpty(persistUser.getFirstName());
            String prevLastName = StringUtils.trimToEmpty(persistUser.getSurname());
            String firstLastCase = StringUtils.trimToEmpty(prevFirstName + " " + prevLastName);
            String lastFirstCase = StringUtils.trimToEmpty(prevLastName + " " + prevFirstName);
            // preventing of removing existing display name pattern match by all empty fields from external service
            if (flags.isFirstNameExternal() || flags.isLastNameExternal()) {
                String syncFirstName = flags.isFirstNameExternal() ? firstName : prevFirstName;
                String syncLastName = flags.isLastNameExternal() ? lastName : prevLastName;
                if (Objects.equals(displayName, firstLastCase)) {
                    displayName = syncFirstName + " " + syncLastName;
                } else if (Objects.equals(displayName, lastFirstCase)) {
                    displayName = syncLastName + " " + syncFirstName;
                }
            }
            displayName = StringUtils.trimToEmpty(displayName);
        }

        persistUser.setFirstName(flags.isFirstNameExternal() ? firstName : persistUser.getFirstName());
        persistUser.setSurname(flags.isLastNameExternal() ? lastName : persistUser.getSurname());
        persistUser.setEmail(flags.isEmailExternal() ? email : persistUser.getEmail());
        persistUser.setDisplayName(displayName);
        persistUser.setPasswordHash(null); // No password is kept from the 3rd parties.
        persistUser.setFlags(UserExternalFlags.builder(flags).getRawFeatures());
        userDao.saveOrUpdate(persistUser);
    }

    public void updateUserData(String user,
                               String firstName,
                               String lastName,
                               String password,
                               String email,
                               String displayName,
                               boolean emailVerified) {
        var persistUser = userDao.getUserByName(user);
        final var currentFlags = persistUser.getUserExternalFlags();
        persistUser.setFirstName(currentFlags.isFirstNameExternal() ? persistUser.getFirstName() : firstName);
        persistUser.setSurname(currentFlags.isLastNameExternal() ? persistUser.getSurname() : lastName);
        persistUser.setEmail(currentFlags.isEmailExternal() ? persistUser.getEmail() : email);
        persistUser.setDisplayName(currentFlags.isDisplayNameExternal() ? persistUser.getDisplayName() : displayName);
        persistUser.setFlags(UserExternalFlags.builder(persistUser.getFlags())
                .applyFeature(Feature.EMAIL_VERIFIED, emailVerified)
                .getRawFeatures());
        if (StringUtils.isNotBlank(password)) {
            persistUser.setPasswordHash(passwordEncoder.encode(password));
        }
        userDao.update(persistUser);
    }

    @Transactional
    public void updateAuthorities(String user, Set<String> authorities) {
        doUpdateAuthorities(user, authorities);
    }

    @Transactional
    public void updateAuthorities(final String user, final Set<String> authorities, final boolean leaveAdminGroups) {
        var fullAuthorities = new HashSet<String>(authorities);
        if (leaveAdminGroups) {
            var currentGroups = userDao.getGroupsForUser(user);
            var currentAdminGroups = getCurrentAdminGroups(currentGroups);
            fullAuthorities.addAll(currentAdminGroups);
        }
        doUpdateAuthorities(user, fullAuthorities);
    }

    private void doUpdateAuthorities(String user, Set<String> authorities) {
        var groups = new HashSet<Group>();
        if (authorities != null) {
            for (String auth : authorities) {
                var group = groupDao.getGroupByName(auth);
                if (group != null) {
                    groups.add(group);
                }
            }
        }
        userDao.updateGroupsForUser(user, groups);
    }

    public Set<String> getCurrentAdminGroups(final Set<Group> groups) {
        var groupNames = new HashSet<String>();

        for (Group group : groups) {
            SimpleGroup simpleGroup = PrivilegesEvaluator.wrap(group);
            if (simpleGroup.hasPrivilege(Privileges.ADMIN.getAuthority())) {
                groupNames.add(group.getName());
            }
        }

        return groupNames;
    }

    public void deleteUser(String username) {
        userDao.deleteUserByName(username);
        if (aclService != null) {
            aclService.deleteSid(new PrincipalSid(username));
        }
    }

    /**
     * Remembers the current time as the last successful sign-in time of the user.
     *
     * <p>Does nothing when the user is unknown. Only the sign-in time is written, so a sign-in
     * cannot overwrite user data edited concurrently.
     *
     * @param username login name of the signed-in user
     */
    public void recordLastLoginTime(String username) {
        userDao.updateLastLoginTime(username, Instant.now());
    }

    /**
     * Check is user has any active session
     *
     * @param username username
     * @return {@code true} if action session found, otherwise {@code false}
     */
    public boolean isUserOnline(String username) {
        return sessionRegistry.getAllPrincipals().stream()
                .map(principal -> {
                    if (principal instanceof UserDetails userDetails) {
                        if (Objects.equals(username, userDetails.getUsername())) {
                            return principal;
                        }
                    } else if (principal instanceof AuthenticatedPrincipal authPrincipal) {
                        if (Objects.equals(username, authPrincipal.getName())) {
                            return principal;
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .map(principal -> !sessionRegistry.getAllSessions(principal, false).isEmpty())
                .orElse(Boolean.FALSE);
    }

    private org.openl.rules.security.User createSecurityUser(User user) {
        var groups = userDao.getGroupsForUser(user.getLoginName());
        return createSecurityUser(user, groups);
    }

    private org.openl.rules.security.User createSecurityUser(User user, Set<Group> groups) {
        return SimpleUser.builder()
                .setFirstName(user.getFirstName())
                .setLastName(user.getSurname())
                .setUsername(user.getLoginName())
                .setPasswordHash(user.getPasswordHash())
                .setPrivileges(PrivilegesEvaluator.createPrivileges(groups))
                .setEmail(user.getEmail())
                .setDisplayName(user.getDisplayName())
                .setExternalFlags(user.getUserExternalFlags())
                .setLastLoginTime(user.getLastLoginTime())
                .build();
    }

    @Transactional(readOnly = true)
    public Set<String> getUserNames() {
        return userDao.getUserNames();
    }
}
