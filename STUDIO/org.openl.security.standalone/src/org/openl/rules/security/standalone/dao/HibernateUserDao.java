package org.openl.rules.security.standalone.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.springframework.transaction.annotation.Transactional;

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
        getSession().createNativeQuery("delete from OpenL_Users where loginName = :name")
                .setParameter("name", name)
                .executeUpdate();
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
        session.createQuery(deleteCriteria).executeUpdate();

        // Insert new mappings
        if (groups != null) {
            for (Group group : groups) {
                session.save(new UserGroup(loginName, group.getId()));
            }
        }
    }
}
