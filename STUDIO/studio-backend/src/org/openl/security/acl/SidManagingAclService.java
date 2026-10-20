package org.openl.security.acl;

import java.util.List;
import java.util.Map;

import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

/**
 * A mutable ACL service that also deletes a security identity together with every entry granted to it, and finds the
 * permissions it is granted.
 */
public interface SidManagingAclService extends MutableAclService {

    void deleteSid(Sid sid);

    /**
     * The permissions granted to the security identity, by object, in the order the objects were made.
     *
     * <p>An object the identity is granted nothing on is left out. An identity the ACL does not know holds none.
     */
    Map<ObjectIdentity, List<Permission>> findPermissions(Sid sid);

}
