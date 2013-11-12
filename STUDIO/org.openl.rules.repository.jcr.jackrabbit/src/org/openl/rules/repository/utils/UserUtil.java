package org.openl.rules.repository.utils;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;

public class UserUtil {
    private TransientRepository repository;
    private Session session;

    public UserUtil(TransientRepository repository) {
        this.repository = repository;
    }

    private Session getSession() throws RepositoryException {
        SimpleCredentials sc = new SimpleCredentials("admin", "admin".toCharArray());
        return this.repository.login(sc);
    }

    public void createNewAdminUser(String name, String password) throws RepositoryException {
        Session session = getSession();
        UserManager userManager = ((JackrabbitSession)session).getUserManager();
        AccessControlManager accessControlManager =  (JackrabbitAccessControlManager) ((JackrabbitSession) session).getAccessControlManager();

        User user = userManager.createUser(name, password, new PrincipalImpl(name) , null);

        AccessControlPolicy[] accessControlPolicies = accessControlManager.getPolicies(session.getRootNode().getPath());

        /*Delete all priveleges from root node*/
        for (int i = 0; i < accessControlPolicies.length; i++) {
            AccessControlList acl = ((AccessControlList)accessControlPolicies[i]);

            for (AccessControlEntry ace : acl.getAccessControlEntries()) {
                acl.removeAccessControlEntry(ace);
            }

            accessControlManager.setPolicy(session.getRootNode().getPath(), acl);
        }

        session.save();

        accessControlPolicies = accessControlManager.getPolicies(session.getRootNode().getPath());
        Privilege[] privileges = new Privilege[]{accessControlManager.privilegeFromName(Privilege.JCR_ALL)};

        /*Set admin right of new user for root node*/
        for (int i = 0; i < accessControlPolicies.length; i++) {
            AccessControlList acl = ((AccessControlList)accessControlPolicies[i]);
            acl.addAccessControlEntry(new PrincipalImpl(name), privileges);
            accessControlManager.setPolicy(session.getRootNode().getPath(), acl);
        }

        session.save();
    }

    public boolean disableAnonymous() {
        try {
            Session session = getSession();
            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            User authorizable = (User) userManager.getAuthorizable("anonymous");

            authorizable.disable("prevent anonymous login");

            session.save();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean changeAdminPass(String pass) {
        try {
            Session session = getSession();
            UserManager userManager = ((JackrabbitSession) session).getUserManager();
            User authorizable = (User) userManager.getAuthorizable("admin");

            authorizable.changePassword(pass);

            session.save();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
