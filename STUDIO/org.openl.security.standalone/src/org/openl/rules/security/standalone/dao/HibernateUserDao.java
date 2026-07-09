package org.openl.rules.security.standalone.dao;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.persistence.ExternalGroup;
import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.persistence.User;
import org.openl.rules.security.standalone.persistence.UserGroup;

/**
 * Hibernate implementation of {@link UserDao}.
 *
 * @author Andrey Naumenko
 * @author Andrei Astrouski
 */
public class HibernateUserDao extends BaseHibernateDao<User> implements UserDao {

    @Override
    @Transactional
    public User getUserByName(final String name) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<User> criteria = builder.createQuery(User.class);
        Root<User> u = criteria.from(User.class);
        criteria.select(u).where(builder.equal(u.get("loginName"), name)).distinct(true);
        List<User> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.getFirst();
    }

    @Override
    @Transactional
    public boolean existsByName(String name) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<User> u = query.from(User.class);

        query.select(cb.count(u)).where(cb.equal(u.get("loginName"), name)).distinct(true);

        return getSession().createQuery(query).getSingleResult() > 0;
    }

    @Override
    @Transactional
    public void deleteUserByName(final String name) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var delete = cb.createCriteriaDelete(User.class);
        var root = delete.from(User.class);
        delete.where(cb.equal(root.get("loginName"), name));
        session.createMutationQuery(delete).executeUpdate();
    }

    @Override
    @Transactional
    public void updateLastLoginTime(String loginName, Instant lastLoginTime) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var update = cb.createCriteriaUpdate(User.class);
        var root = update.from(User.class);
        update.set("lastLoginTime", lastLoginTime);
        update.where(cb.equal(root.get("loginName"), loginName));
        session.createMutationQuery(update).executeUpdate();
    }

    @Override
    @Transactional
    public List<User> getAllUsers() {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<User> criteria = builder.createQuery(User.class);
        Root<User> root = criteria.from(User.class);
        criteria.select(root).orderBy(builder.asc(builder.upper(root.get("loginName"))));
        return getSession().createQuery(criteria).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersInGroup(String groupName) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(User.class);
        var root = criteria.from(User.class);
        criteria.select(root)
                .where(belongsToGroup(builder, criteria, root, groupName))
                .orderBy(builder.asc(builder.upper(root.get("loginName"))));
        return getSession().createQuery(criteria).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersInGroup(String groupName) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(Long.class);
        var root = criteria.from(User.class);
        criteria.select(builder.count(root)).where(belongsToGroup(builder, criteria, root, groupName));
        return getSession().createQuery(criteria).getSingleResult();
    }

    /**
     * Matches the users belonging to the group either by the direct assignment or by a matched
     * external group with the same name.
     */
    private static Predicate belongsToGroup(CriteriaBuilder builder,
                                            AbstractQuery<?> query,
                                            Root<User> root,
                                            String groupName) {
        // Subquery: users directly assigned to the group
        var internal = query.subquery(String.class);
        var ugRoot = internal.from(UserGroup.class);
        var groupSubquery = internal.subquery(Long.class);
        var groupRoot = groupSubquery.from(Group.class);
        groupSubquery.select(groupRoot.get("id")).where(builder.equal(groupRoot.get("name"), groupName));
        internal.select(ugRoot.get("id").get("loginName"))
                .where(ugRoot.get("id").get("groupId").in(groupSubquery));

        // Subquery: users having a matched external group with the same name
        var external = query.subquery(String.class);
        var egRoot = external.from(ExternalGroup.class);
        external.select(egRoot.get("loginName")).where(builder.equal(egRoot.get("groupName"), groupName));

        return builder.or(root.get("loginName").in(internal), root.get("loginName").in(external));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findUserNames(String searchTerm, int limit) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(String.class);
        var root = criteria.from(User.class);
        criteria.select(root.get("loginName"))
                .where(builder.like(builder.lower(root.get("loginName")),
                        "%" + escape(searchTerm) + "%",
                        builder.literal(ESCAPE_CHAR)))
                .orderBy(builder.asc(builder.upper(root.get("loginName"))));
        return getSession().createQuery(criteria).setMaxResults(limit).getResultList();
    }

    @Override
    public Set<String> getUserNames() {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(String.class);
        var root = criteria.from(User.class);
        criteria.select(root.get("loginName"));
        criteria.orderBy(builder.asc(builder.upper(root.get("loginName"))));
        return getSession().createQuery(criteria).getResultStream()
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Group> getGroupsForUser(String loginName) {
        var builder = getSession().getCriteriaBuilder();

        // Subquery: SELECT ug.id.groupId FROM UserGroup ug WHERE ug.id.loginName = :loginName
        var criteria = builder.createQuery(Group.class);
        var groupRoot = criteria.from(Group.class);
        var subquery = criteria.subquery(Long.class);
        var ugRoot = subquery.from(UserGroup.class);
        subquery.select(ugRoot.get("id").get("groupId"))
                .where(builder.equal(ugRoot.get("id").get("loginName"), loginName));

        criteria.select(groupRoot).where(groupRoot.get("id").in(subquery));
        return new HashSet<>(getSession().createQuery(criteria).getResultList());
    }

    @Override
    @Transactional
    public void updateGroupsForUser(String loginName, Set<Group> groups) {
        var session = getSession();
        var builder = session.getCriteriaBuilder();

        // Delete existing mappings
        var deleteCriteria = builder.createCriteriaDelete(UserGroup.class);
        var deleteRoot = deleteCriteria.from(UserGroup.class);
        deleteCriteria.where(builder.equal(deleteRoot.get("id").get("loginName"), loginName));
        session.createMutationQuery(deleteCriteria).executeUpdate();

        // Insert new mappings
        if (groups != null) {
            for (Group group : groups) {
                session.persist(new UserGroup(loginName, group.getId()));
            }
        }
    }

}
