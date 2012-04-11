package org.openl.rules.security.standalone.dao;

import org.hibernate.criterion.Restrictions;

import org.openl.rules.security.standalone.persistence.User;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate implementation of {@link UserDao}.
 *
 * @author Andrey Naumenko
 */
public class HibernateUserDao extends BaseHibernateDao implements UserDao {
    public HibernateUserDao() {
        super(User.class);
    }

    @Transactional
    public User getUserByName(final String name) {
        return (User) getSession().createCriteria(User.class).add(Restrictions.eq("loginName", name)).uniqueResult();
    }
}
