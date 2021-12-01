package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.openl.rules.security.standalone.persistence.ExternalGroup;
import org.openl.rules.security.standalone.persistence.Group;

public interface ExternalGroupDao extends Dao<ExternalGroup> {

    void deleteAllForUser(String loginName);

    List<ExternalGroup> findAllForUser(String loginName);

    long countAllForUser(String loginName);

    List<Group> findMatchedForUser(String loginName);

    long countMatchedForUser(String loginName);

    List<ExternalGroup> findNotMatchedForUser(String loginName);

    long countNotMatchedForUser(String loginName);

    List<String> findAllByName(String groupName, int limit);
}
