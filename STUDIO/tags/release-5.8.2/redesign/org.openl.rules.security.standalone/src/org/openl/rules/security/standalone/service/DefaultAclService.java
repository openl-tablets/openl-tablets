package org.openl.rules.security.standalone.service;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import org.hibernate.criterion.Restrictions;

import org.openl.rules.security.standalone.persistence.AccessControlEntry;

import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class DefaultAclService extends HibernateDaoSupport {

    public AccessControlEntry getAccessControlEntry(final String userName, final String resource) {
        return (AccessControlEntry) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                return session.createCriteria(AccessControlEntry.class).add(Restrictions.eq("loginName", userName))
                        .add(Restrictions.eq("resource", resource)).uniqueResult();
            }
        });
    }
}
