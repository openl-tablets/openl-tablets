package org.openl.rules.webstudio.service;

import java.util.Collection;
import java.util.List;

import org.openl.rules.security.Privilege;

/**
 * External Groups service
 * 
 * @author Vladyslav Pikus
 */
public interface ExternalGroupService {

    /**
     * Delete all external groups by given login name
     * 
     * @param loginName username
     */
    void deleteAllForUser(String loginName);

    /**
     * Fully replace old user external groups with new ones. Orphan groups will be deleted.
     * 
     * @param loginName username
     * @param externalGroups collections of new external groups.
     */
    void mergeAllForUser(String loginName, Collection<Privilege> externalGroups);

    /**
     * Find all external groups for user
     *
     * @param loginName username
     * @return found collection of external group for user
     */
    List<org.openl.rules.security.Group> findAllForUser(String loginName);

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
    List<org.openl.rules.security.Group> findMatchedForUser(String loginName);

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
    List<org.openl.rules.security.Group> findNotMatchedForUser(String loginName);

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
    List<org.openl.rules.security.Group> findAllByName(String groupName, int limit);
}
