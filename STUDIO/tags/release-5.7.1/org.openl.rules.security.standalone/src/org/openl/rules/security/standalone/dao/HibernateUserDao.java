package org.openl.rules.security.standalone.dao;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.hibernate.criterion.Restrictions;

import org.openl.rules.security.standalone.persistence.User;

import org.springframework.orm.hibernate3.HibernateCallback;

/**
 * Hibernate implementation of {@link UserDao}.
 *
 * @author Andrey Naumenko
 */
public class HibernateUserDao extends BaseHibernateDao implements UserDao {
    public HibernateUserDao() {
        super(User.class);
    }

    /**
     * {@inheritDoc}
     */
    public User getUserByName(final String name) {
        return (User) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                return session.createCriteria(User.class).add(Restrictions.eq("loginName", name)).uniqueResult();
            }
        });
    }
}
