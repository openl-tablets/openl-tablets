package org.openl.rules.security.standalone.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

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
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<User> criteria = builder.createQuery(User.class);
        Root<User> u = criteria.from(User.class);
        criteria.select(u).where(builder.equal(u.get("loginName"), name)).distinct(true);
        List<User> results = getSession().createQuery(criteria).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
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
}
