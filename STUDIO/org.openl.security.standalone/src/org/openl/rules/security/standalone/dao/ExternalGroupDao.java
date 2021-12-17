package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.openl.rules.security.standalone.persistence.ExternalGroup;
import org.openl.rules.security.standalone.persistence.Group;

/**
 * External Groups data access object.
 *
 * @author Vladyslav Pikus
 */
public interface ExternalGroupDao extends Dao<ExternalGroup> {

    /**
     * Delete all external groups
     */
    void deleteAll();

    /**
     * Delete all external groups by given login name
     *
     * @param loginName username
     */
    void deleteAllForUser(String loginName);

    /**
     * Find all external groups for user
     *
     * @param loginName username
     * @return found collection of external group for user
     */
    List<ExternalGroup> findAllForUser(String loginName);

    /**
     * Count all external groups for user
     *
     * @param loginName username
     * @return positive number of external groups otherwise {@code 0}
     */
    long countAllForUser(String loginName);

    /**
     * Find all matched external groups for user. By matched means that the same internal group exists
     *
     * @param loginName username
     * @return found collection of external group for user
     */
    List<Group> findMatchedForUser(String loginName);

    /**
     * Find all matched external groups for user. By matched means that the same internal groups exist
     *
     * @param loginName username
     * @return positive number of matched external groups otherwise {@code 0}
     */
    long countMatchedForUser(String loginName);

    /**
     * Find all not matched external groups for user. By not matched means that the same internal group doesn't exit
     *
     * @param loginName username
     * @return found collection of external group for user
     */
    List<ExternalGroup> findNotMatchedForUser(String loginName);

    /**
     * Find all not matched external groups for user. By not matched means that the same internal group doesn't exit
     *
     * @param loginName username
     * @return positive number of not matched external groups otherwise {@code 0}
     */
    long countNotMatchedForUser(String loginName);

    /**
     * Search external groups by full group name or fragment
     *
     * @param groupName full group name or term fragment
     * @param limit max results number
     * @return collection of found external groups
     */
    List<String> findAllByName(String groupName, int limit);
}
