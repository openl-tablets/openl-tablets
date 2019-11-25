package org.openl.rules.security.standalone.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.openl.rules.security.standalone.persistence.Group;
import org.springframework.transaction.annotation.Transactional;

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
    public void deleteGroupByName(final String name) {
        getSession().createNativeQuery(
            "delete from OpenL_Group2Group where includedGroupID = (select id from OpenL_Groups where groupName = :name)")
            .setParameter("name", name)
            .executeUpdate();
        getSession().createNativeQuery("delete from OpenL_Groups where groupName = :name")
            .setParameter("name", name)
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
