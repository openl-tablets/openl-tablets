package org.openl.rules.webstudio.service;

import java.util.Collection;
import java.util.List;

import org.openl.rules.security.Privilege;

public interface ExternalGroupService {

    void deleteAllForUser(String loginName);

    void mergeAllForUser(String loginName, Collection<Privilege> externalGroups);

    List<org.openl.rules.security.Group> findAllForUser(String loginName);

    long countAllForUser(String loginName);

    List<org.openl.rules.security.Group> findMatchedForUser(String loginName);

    long countMatchedForUser(String loginName);

    List<org.openl.rules.security.Group> findNotMatchedForUser(String loginName);

    long countNotMatchedForUser(String loginName);
}
