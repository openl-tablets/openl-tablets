package org.openl.rules.security.standalone.dao;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.persistence.User;

/**
 * Hibernate implementation of {@link GroupDao}.
 *
 * @author Andrey Naumenko
 */
public class HibernateGroupDao extends BaseHibernateDao<Group> implements GroupDao {

    @Override
    @Transactional
    public Group getGroupByName(final String name) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<Group> criteria = builder.createQuery(Group.class);
        Root<Group> g = criteria.from(Group.class);
        criteria.select(g).where(builder.equal(g.get("name"), name)).distinct(true);
        List<Group> groupList = getSession().createQuery(criteria).getResultList();
        return groupList.isEmpty() ? null : groupList.get(0);
    }

    @Override
    @Transactional
    public boolean existsByName(String name) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<Group> groupRoot = criteria.from(Group.class);

        criteria.select(builder.count(groupRoot))
                .where(builder.equal(groupRoot.get("name"), name));
        try {
            var count = getSession().createQuery(criteria).setMaxResults(1).getSingleResult();
            return count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public Group getGroupById(final Long id) {
        return getSession().get(Group.class, id);
    }

    @Override
    @Transactional
    public void deleteGroupById(final Long id) {
        getSession().createNativeQuery("delete from OpenL_Groups where id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    @Transactional
    public List<Group> getAllGroups() {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<Group> criteria = builder.createQuery(Group.class);
        Root<Group> root = criteria.from(Group.class);
        criteria.select(root).orderBy(builder.asc(builder.upper(root.get("name"))));
        return getSession().createQuery(criteria).getResultList();
    }

    @Override
    public long countUsersInGroup(String groupName) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(Long.class);
        var userRoot = criteria.from(User.class);

        criteria.select(builder.count(userRoot))
                .where(builder.equal(userRoot.join("groups").get("name"), groupName));

        return getSession().createQuery(criteria).getSingleResult();
    }

    @Override
    public Set<String> getGroupNames() {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(String.class);
        var root = criteria.from(Group.class);
        criteria.select(root.get("name")).distinct(true);
        return getSession().createQuery(criteria).getResultStream()
                .collect(Collectors.toSet());
    }

    @Override
    public void deleteGroupByName(String name) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createCriteriaDelete(Group.class);
        var root = criteria.from(Group.class);
        criteria.where(builder.equal(root.get("name"), name));
        getSession().createQuery(criteria).executeUpdate();
    }
}
