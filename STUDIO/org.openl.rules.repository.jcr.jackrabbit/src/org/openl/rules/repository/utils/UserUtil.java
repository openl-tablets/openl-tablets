package org.openl.rules.repository.utils;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.security.*;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserUtil {
    private final Logger log = LoggerFactory.getLogger(UserUtil.class);

    private TransientRepository repository;

    public UserUtil(TransientRepository repository) {
        this.repository = repository;
    }

    private Session createSession() throws RepositoryException {
        SimpleCredentials sc = new SimpleCredentials("admin", "admin".toCharArray());
        return this.repository.login(sc);
    }

    public void createNewAdminUser(String name, String password) throws RepositoryException {
        Session session = null;
        try {
            session = createSession();

            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            AccessControlManager accessControlManager = session.getAccessControlManager();

            userManager.createUser(name, password, new PrincipalImpl(name), null);

            AccessControlPolicy[] accessControlPolicies = accessControlManager
                .getPolicies(session.getRootNode().getPath());

            /* Delete all priveleges from root node */
            for (AccessControlPolicy accessControlPolicy : accessControlPolicies) {
                AccessControlList acl = ((AccessControlList) accessControlPolicy);

                for (AccessControlEntry ace : acl.getAccessControlEntries()) {
                    acl.removeAccessControlEntry(ace);
                }

                accessControlManager.setPolicy(session.getRootNode().getPath(), acl);
            }

            session.save();

            accessControlPolicies = accessControlManager.getPolicies(session.getRootNode().getPath());
            Privilege[] privileges = new Privilege[] { accessControlManager.privilegeFromName(Privilege.JCR_ALL) };

            /* Set admin right of new user for root node */
            for (AccessControlPolicy accessControlPolicy : accessControlPolicies) {
                AccessControlList acl = ((AccessControlList) accessControlPolicy);
                acl.addAccessControlEntry(new PrincipalImpl(name), privileges);
                accessControlManager.setPolicy(session.getRootNode().getPath(), acl);
            }

            session.save();
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    public boolean disableAnonymous() {
        Session session = null;
        try {
            session = createSession();
            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            User authorizable = (User) userManager.getAuthorizable("anonymous");

            authorizable.disable("prevent anonymous login");

            session.save();
            return true;
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            return false;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    public boolean changeAdminPass(String pass) {
        Session session = null;
        try {
            session = createSession();
            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            User authorizable = (User) userManager.getAuthorizable("admin");

            authorizable.changePassword(pass);

            session.save();
            return true;
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            return false;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }
}
