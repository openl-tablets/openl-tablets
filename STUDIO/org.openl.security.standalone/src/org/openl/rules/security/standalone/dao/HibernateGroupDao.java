package org.openl.rules.security.standalone.dao;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.persistence.Group;

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
}
