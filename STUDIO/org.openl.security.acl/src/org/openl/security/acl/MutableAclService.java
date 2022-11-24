package org.openl.security.acl;

import org.springframework.security.acls.model.Sid;

public interface MutableAclService extends org.springframework.security.acls.model.MutableAclService {

    void deleteSid(Sid sid, Sid newOwner);

}
