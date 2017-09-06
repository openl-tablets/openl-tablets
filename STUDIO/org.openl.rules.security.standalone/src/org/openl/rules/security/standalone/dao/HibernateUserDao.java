package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openl.rules.security.standalone.persistence.User;
import org.springframework.transaction.annotation.Transactional;

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
        return (User) getSession().createCriteria(User.class).add(Restrictions.eq("loginName", name)).uniqueResult();
    }

    @Override
    @Transactional
    public void deleteUserByName(final String name) {
        getSession().createSQLQuery("delete from OpenL_Users where loginName = :name").setString("name", name).executeUpdate();
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<User> getAllUsers() {
        return getSession().createCriteria(User.class)
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                .addOrder(Order.asc("loginName"))
                .list();
    }
}
