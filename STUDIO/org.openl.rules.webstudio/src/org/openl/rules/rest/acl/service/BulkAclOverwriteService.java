package org.openl.rules.rest.acl.service;

import org.openl.rules.rest.acl.model.BulkAclOverwriteRequest;

public interface BulkAclOverwriteService {

    /**
     * Overwrites ACL entries for multiple resources.
     *
     * @param request the bulk ACL overwrite request containing resource access configurations
     */
    void process(BulkAclOverwriteRequest request);

}
