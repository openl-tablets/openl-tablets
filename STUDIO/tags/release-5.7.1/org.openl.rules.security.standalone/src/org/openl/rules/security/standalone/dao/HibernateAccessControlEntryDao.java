package org.openl.rules.security.standalone.dao;

import org.openl.rules.security.standalone.persistence.AccessControlEntry;

/**
 * Hibernate implementation of {@link AccessControlEntryDao}.
 *
 * @author Andrey Naumenko
 */
public class HibernateAccessControlEntryDao extends BaseHibernateDao implements AccessControlEntryDao {
    public HibernateAccessControlEntryDao() {
        super(AccessControlEntry.class);
    }
}
