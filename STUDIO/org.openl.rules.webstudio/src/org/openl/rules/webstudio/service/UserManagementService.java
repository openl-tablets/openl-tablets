package org.openl.rules.webstudio.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.UserExternalFlags;
import org.openl.rules.security.UserExternalFlags.Feature;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.persistence.User;
import org.openl.util.StringUtils;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Andrei Astrouski
 */
@Service("userManagementService")
public class UserManagementService {

    private final UserDao userDao;
    private final GroupDao groupDao;
    private final SessionRegistry sessionRegistry;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserDao userDao, GroupDao groupDao, SessionRegistry sessionRegistry, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.groupDao = groupDao;
        this.sessionRegistry = sessionRegistry;
        this.passwordEncoder = passwordEncoder;
    }

    public List<org.openl.rules.security.User> getAllUsers() {
        return userDao.getAllUsers().stream().map(this::createSecurityUser).collect(Collectors.toList());
    }

    public org.openl.rules.security.User getUser(String username) {
        return Optional.ofNullable(userDao.getUserByName(username)).map(this::createSecurityUser).orElse(null);
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
            String displayName,
            UserExternalFlags externalFlags) {
        User persistUser = new User();
        persistUser.setLoginName(user);
        persistUser.setPasswordHash(StringUtils.isNotBlank(password) ? passwordEncoder.encode(password) : null);
        persistUser.setFirstName(firstName);
        persistUser.setSurname(lastName);
        persistUser.setEmail(email);
        persistUser.setDisplayName(displayName);
        persistUser.setFlags(UserExternalFlags.builder(externalFlags)
            .withoutFeature(Feature.SYNC_EXTERNAL_GROUPS) // Disable transient feature
            .getRawFeatures());

        userDao.save(persistUser);
    }

    public void updateUserData(String user,
            String firstName,
            String lastName,
            String email,
            String displayName,
            UserExternalFlags externalFlags) {
        User persistUser = userDao.getUserByName(user);
        final UserExternalFlags currentFlags = persistUser.getUserExternalFlags();
        persistUser.setFirstName(currentFlags.isFirstNameExternal() ? persistUser.getFirstName() : firstName);
        persistUser.setSurname(currentFlags.isLastNameExternal() ? persistUser.getSurname() : lastName);
        persistUser.setEmail(currentFlags.isEmailExternal() ? persistUser.getEmail() : email);
        persistUser.setDisplayName(currentFlags.isDisplayNameExternal() ? persistUser.getDisplayName() : displayName);
        persistUser.setFlags(UserExternalFlags.builder(externalFlags).getRawFeatures());
        userDao.update(persistUser);
    }

    public void updateUserData(String user,
            String firstName,
            String lastName,
            String password,
            String email,
            String displayName,
            boolean emailVerified) {
        User persistUser = userDao.getUserByName(user);
        final UserExternalFlags currentFlags = persistUser.getUserExternalFlags();
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

    public void updateAuthorities(String user, Set<String> authorities) {
        User persistUser = userDao.getUserByName(user);
        Set<Group> groups = new HashSet<>();
        if (authorities != null) {
            for (String auth : authorities) {
                groups.add(groupDao.getGroupByName(auth));
            }
        }
        persistUser.setGroups(groups);

        userDao.update(persistUser);
    }

    public void updateAuthorities(final String user, final Set<String> authorities, final boolean leaveAdminGroups) {
        Set<String> fullAuthorities = new HashSet<>(authorities);
        if (leaveAdminGroups) {
            User persistUser = userDao.getUserByName(user);
            Set<Group> currentGroups = persistUser.getGroups();
            Set<String> currentAdminGroups = getCurrentAdminGroups(currentGroups);
            fullAuthorities.addAll(currentAdminGroups);
        }
        updateAuthorities(user, fullAuthorities);
    }

    public Set<String> getCurrentAdminGroups(final Set<Group> groups) {
        Set<String> groupNames = new HashSet<>();

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
    }

    /**
     * Check is user has any active session
     *
     * @param username username
     * @return {@code true} if action session found, otherwise {@code false}
     */
    public boolean isUserOnline(String username) {
        return sessionRegistry.getAllPrincipals().stream().map(principal -> {
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                if (Objects.equals(username, userDetails.getUsername())) {
                    return principal;
                }
            } else if (principal instanceof AuthenticatedPrincipal) {
                AuthenticatedPrincipal authPrincipal = (AuthenticatedPrincipal) principal;
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
        return SimpleUser.builder()
                .setFirstName(user.getFirstName())
                .setLastName(user.getSurname())
                .setUsername(user.getLoginName())
                .setPasswordHash(user.getPasswordHash())
                .setPrivileges(PrivilegesEvaluator.createPrivileges(user))
                .setEmail(user.getEmail())
                .setDisplayName(user.getDisplayName())
                .setExternalFlags(user.getUserExternalFlags())
                .build();
    }
}
