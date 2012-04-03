package org.openl.rules.security.standalone.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openl.rules.security.standalone.persistence.PersistentObject;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convenient base implementation of {@link Dao} using Hibernate.
 *
 * @author Andrey Naumenko
 */
public abstract class BaseHibernateDao implements Dao {

    private Class persistentClass;

    private SessionFactory sessionFactory;

    public BaseHibernateDao(Class persistentClass) {
        this.persistentClass = persistentClass;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    @Transactional
    public boolean canBeDeleted(Object obj) {
        return true;
    }

    @Transactional
    public void delete(Object obj) {
        getSession().delete(getSession().load(obj.getClass(), ((PersistentObject) obj).getId()));
    }

    @Transactional
    public Object getById(Long id) {
        return getSession().get(persistentClass, id);
    }

    @Transactional
    public Object loadById(Long id) {
        return getSession().load(persistentClass, id);
    }

    @Transactional
    public void save(Object obj) {
        getSession().save(obj);
    }

    @Transactional
    public void saveOrUpdate(Object obj) {
        getSession().saveOrUpdate(obj);
    }

    @Transactional
    public void update(Object obj) {
        getSession().update(obj);
    }

}
