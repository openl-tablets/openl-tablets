package org.openl.rules.security.standalone.dao;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.JoinType;

import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.persistence.Group;
import org.openl.rules.security.standalone.persistence.UserGroup;

/**
 * Hibernate implementation of {@link GroupDao}.
 *
 * @author Andrey Naumenko
 */
public class HibernateGroupDao extends BaseHibernateDao<Group> implements GroupDao {

    private static final String ID = "id";
    private static final String NAME = "name";

    @Override
    @Transactional
    public Group getGroupByName(final String name) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(Group.class);
        var g = criteria.from(Group.class);
        criteria.select(g).where(builder.equal(g.get(NAME), name)).distinct(true);
        List<Group> groupList = getSession().createQuery(criteria).getResultList();
        return groupList.isEmpty() ? null : groupList.getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Group> getGroupsByNames(Collection<String> names) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(Group.class);
        var root = criteria.from(Group.class);
        // The privileges come with the groups, not in one query more for each group.
        root.fetch("privileges", JoinType.LEFT);
        criteria.select(root).where(root.get(NAME).in(names)).distinct(true);
        return getSession().createQuery(criteria).getResultList();
    }

    @Override
    @Transactional
    public boolean existsByName(String name) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(Long.class);
        var groupRoot = criteria.from(Group.class);

        criteria.select(builder.count(groupRoot))
                .where(builder.equal(groupRoot.get(NAME), name));
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
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var delete = cb.createCriteriaDelete(Group.class);
        var root = delete.from(Group.class);
        delete.where(cb.equal(root.get(ID), id));
        session.createMutationQuery(delete).executeUpdate();
    }

    @Override
    @Transactional
    public List<Group> getAllGroups() {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(Group.class);
        var root = criteria.from(Group.class);
        criteria.select(root).orderBy(builder.asc(builder.upper(root.get(NAME))));
        return getSession().createQuery(criteria).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findGroupNames(String searchTerm, int limit) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(String.class);
        var root = criteria.from(Group.class);
        criteria.select(root.get(NAME))
                .where(builder.like(builder.lower(root.get(NAME)),
                        "%" + escape(searchTerm) + "%",
                        builder.literal(ESCAPE_CHAR)))
                .orderBy(builder.asc(builder.upper(root.get(NAME))));
        return getSession().createQuery(criteria).setMaxResults(limit).getResultList();
    }

    @Override
    public long countUsersInGroup(String groupName) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(Long.class);
        var ugRoot = criteria.from(UserGroup.class);

        // Subquery: SELECT g.id FROM Group g WHERE g.name = :groupName
        var subquery = criteria.subquery(Long.class);
        var groupRoot = subquery.from(Group.class);
        subquery.select(groupRoot.get(ID))
                .where(builder.equal(groupRoot.get(NAME), groupName));

        criteria.select(builder.count(ugRoot))
                .where(ugRoot.get(ID).get("groupId").in(subquery));
        return getSession().createQuery(criteria).getSingleResult();
    }

    @Override
    public Set<String> getGroupNames() {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createQuery(String.class);
        var root = criteria.from(Group.class);
        criteria.select(root.get(NAME)).distinct(true);
        return getSession().createQuery(criteria).getResultStream()
                .collect(Collectors.toSet());
    }

    @Override
    public void deleteGroupByName(String name) {
        var builder = getSession().getCriteriaBuilder();
        var criteria = builder.createCriteriaDelete(Group.class);
        var root = criteria.from(Group.class);
        criteria.where(builder.equal(root.get(NAME), name));
        getSession().createMutationQuery(criteria).executeUpdate();
    }

}
