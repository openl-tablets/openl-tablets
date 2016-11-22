package org.openl.rules.repository.db;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JndiDBRepositoryFactory extends DatasourceDBRepositoryFactory {
    public JndiDBRepositoryFactory(String uri, String login, String password) {
        super(null, login, password);
        InitialContext initialContext = null;
        try {
            initialContext = new InitialContext();
            this.dataSource = (DataSource) initialContext.lookup(uri);
        } catch (NamingException e) {
            throw new IllegalStateException("Cannot determine JNDI [ " + uri + " ] name", e);
        } finally {
            if (initialContext != null) {
                try {
                    initialContext.close();
                } catch (NamingException e) {
                    Logger log = LoggerFactory.getLogger(JndiDBRepositoryFactory.class);
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
}
