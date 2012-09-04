package org.openl.rules.security.standalone.dao;

import org.hibernate.criterion.Restrictions;
import org.openl.rules.security.standalone.persistence.Group;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate implementation of {@link GroupDao}.
 *
 * @author Andrey Naumenko
 */
public class HibernateGroupDao extends BaseHibernateDao<Group> implements GroupDao {

    public HibernateGroupDao() {
        super(Group.class);
    }

    @Override
    @Transactional
    public Group getGroupByName(final String name) {
        return (Group) getSession().createCriteria(Group.class).add(Restrictions.eq("name", name)).uniqueResult();
    }

}
