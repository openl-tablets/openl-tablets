package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convenient base implementation of {@link Dao} using Hibernate.
 *
 * @author Andrey Naumenko
 */
public abstract class BaseHibernateDao<T> implements Dao<T> {

    private Class<T> persistentClass;

    private SessionFactory sessionFactory;

    public BaseHibernateDao(Class<T> persistentClass) {
        this.persistentClass = persistentClass;
    }

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

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public List<T> getAll() {
        return getSession().createCriteria(persistentClass)
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
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
