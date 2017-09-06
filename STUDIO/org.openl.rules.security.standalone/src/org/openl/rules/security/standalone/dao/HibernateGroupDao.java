package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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
        return (Group) getSession().createCriteria(Group.class).add(Restrictions.eq("name", name)).uniqueResult();
    }

    @Override
    @Transactional
    public void deleteGroupByName(final String name) {
        getSession().createSQLQuery("delete from OpenL_Group2Group where includedGroupID = (select id from OpenL_Groups where groupName = :name)").setString("name", name).executeUpdate();
        getSession().createSQLQuery("delete from OpenL_Groups where groupName = :name").setString("name", name).executeUpdate();
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Group> getAllGroups() {
        return getSession().createCriteria(Group.class)
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                .addOrder(Order.asc("name"))
                .list();
    }
}
