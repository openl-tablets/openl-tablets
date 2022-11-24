package org.openl.security.acl;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.Sid;

public class JdbcMutableAclService extends org.springframework.security.acls.jdbc.JdbcMutableAclService implements InitializingBean, MutableAclService {
    public final String jdbcUrl;

    private String deleteSidQuery = "delete from acl_sid where id=?";
    private String updateOwnerQuery = "update acl_object_identity aoi set aoi.owner_sid = ? where aoi.owner_sid = ?";
    private String deleteEntriesBySidQuery = "delete from acl_entry ace where ace.sid=?";

    private final AclCache aclCache;

    public JdbcMutableAclService(DataSource dataSource,
            LookupStrategy lookupStrategy,
            AclCache aclCache,
            String jdbcUrl) {
        super(dataSource, lookupStrategy, aclCache);
        this.jdbcUrl = jdbcUrl;
        this.aclCache = aclCache;
    }

    @Override
    public void afterPropertiesSet() {
        if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:postgresql")) {
            setClassIdentityQuery("select currval(pg_get_serial_sequence('acl_class', 'id'))");
            setSidIdentityQuery("select currval(pg_get_serial_sequence('acl_sid', 'id'))");
        }
    }

    public void deleteSid(Sid sid, Sid newOwner) {
        Long sidId = createOrRetrieveSidPrimaryKey(sid, false);
        if (sidId == null) {
            return;
        }
        this.jdbcOperations.update(deleteEntriesBySidQuery, sidId);
        Long newOwnerSid = createOrRetrieveSidPrimaryKey(newOwner, true);
        this.jdbcOperations.update(updateOwnerQuery, newOwnerSid, sidId);
        this.jdbcOperations.update(deleteSidQuery, sidId);
        this.aclCache.clearCache();
    }

    public void setDeleteSidQuery(String deleteSidQuery) {
        this.deleteSidQuery = deleteSidQuery;
    }

    public void setDeleteEntriesBySidQuery(String deleteEntriesBySidQuery) {
        this.deleteEntriesBySidQuery = deleteEntriesBySidQuery;
    }

    public void setUpdateOwnerQuery(String updateOwnerQuery) {
        this.updateOwnerQuery = updateOwnerQuery;
    }
}
