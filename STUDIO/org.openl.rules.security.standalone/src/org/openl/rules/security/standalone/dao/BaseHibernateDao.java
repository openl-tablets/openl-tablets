package org.openl.rules.security.standalone.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convenient base implementation of {@link Dao} using Hibernate.
 *
 * @author Andrey Naumenko
 */
public abstract class BaseHibernateDao<T> implements Dao<T> {

    private SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    @Transactional
    public void delete(T obj) {
        getSession().delete(obj);
    }

    @Override
    @Transactional
    public void save(T obj) {
        getSession().save(obj);
    }

    @Override
    @Transactional
    public void update(T obj) {
        getSession().update(obj);
    }

    @Override
    @Transactional
    public void merge(T obj) {
        getSession().merge(obj);
    }

}
