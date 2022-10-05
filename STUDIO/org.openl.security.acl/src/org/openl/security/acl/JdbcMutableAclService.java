package org.openl.security.acl;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;

public class JdbcMutableAclService extends org.springframework.security.acls.jdbc.JdbcMutableAclService implements InitializingBean {
    public final String jdbcUrl;

    public JdbcMutableAclService(DataSource dataSource,
            LookupStrategy lookupStrategy,
            AclCache aclCache,
            String jdbcUrl) {
        super(dataSource, lookupStrategy, aclCache);
        this.jdbcUrl = jdbcUrl;
    }

    @Override
    public void afterPropertiesSet() {
        if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:postgresql")) {
            setClassIdentityQuery("select currval(pg_get_serial_sequence('acl_class', 'id'))");
            setSidIdentityQuery("select currval(pg_get_serial_sequence('acl_sid', 'id'))");
        }
    }

}
