package org.openl.security.acl;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.Sid;

public class JdbcMutableAclService extends org.springframework.security.acls.jdbc.JdbcMutableAclService implements InitializingBean, MutableAclService {
    public final String jdbcUrl;

    private String deleteSidQuery = "delete from acl_sid where id=?";
    private String updateOwnerQuery = "update acl_object_identity set owner_sid = ? where owner_sid = ?";
    private String deleteEntriesBySidQuery = "delete from acl_entry where sid=?";
    private String updateSidQuery = "update acl_sid set sid = ? where sid = ? and principal=?";

    private final AclCache aclCache;
    private final Sid relevantSystemWideSid;

    public JdbcMutableAclService(DataSource dataSource,
            LookupStrategy lookupStrategy,
            AclCache aclCache,
            String jdbcUrl,
            Sid relevantSystemWideSid) {
        super(dataSource, lookupStrategy, aclCache);
        this.jdbcUrl = jdbcUrl;
        this.aclCache = aclCache;
        this.relevantSystemWideSid = relevantSystemWideSid;
    }

    @Override
    public void afterPropertiesSet() {
        if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:postgresql")) {
            setClassIdentityQuery("select currval(pg_get_serial_sequence('acl_class', 'id'))");
            setSidIdentityQuery("select currval(pg_get_serial_sequence('acl_sid', 'id'))");
        } else if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:mysql")) {
            setClassIdentityQuery("select @@IDENTITY");
            setSidIdentityQuery("select @@IDENTITY");
        } else if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:oracle")) {
            setClassIdentityQuery("select acl_class_sequence.currval from dual");
            setSidIdentityQuery("select acl_sid_sequence.currval from dual");
        } else if (jdbcUrl != null && jdbcUrl.startsWith("jdbc:sqlserver")) {
            setClassIdentityQuery("select ident_current('acl_class')");
            setSidIdentityQuery("select ident_current('acl_sid')");
        }
    }

    public void deleteSid(Sid sid) {
        Long sidId = createOrRetrieveSidPrimaryKey(sid, false);
        if (sidId == null) {
            return;
        }
        this.jdbcOperations.update(deleteEntriesBySidQuery, sidId);
        Long newOwnerSid = createOrRetrieveSidPrimaryKey(relevantSystemWideSid, true);
        this.jdbcOperations.update(updateOwnerQuery, newOwnerSid, sidId);
        this.jdbcOperations.update(deleteSidQuery, sidId);
        this.aclCache.clearCache();
    }

    public void updateSid(Sid sid, String newSidName) {
        if (sid instanceof GrantedAuthoritySid) {
            this.jdbcOperations
                .update(updateSidQuery, newSidName, ((GrantedAuthoritySid) sid).getGrantedAuthority(), false);
        } else if (sid instanceof PrincipalSid) {
            this.jdbcOperations.update(updateSidQuery, newSidName, ((PrincipalSid) sid).getPrincipal(), true);
        } else {
            throw new IllegalStateException("Sid type is not supported");
        }
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

    public void setUpdateSidQuery(String updateSidQuery) {
        this.updateSidQuery = updateSidQuery;
    }
}
