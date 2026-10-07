package org.openl.security.acl;

import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.Sid;

/**
 * A mutable ACL service that also deletes a security identity together with every entry granted to it.
 */
public interface SidManagingAclService extends MutableAclService {

    void deleteSid(Sid sid);

}
