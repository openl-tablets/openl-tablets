package org.openl.security.acl;

import javax.sql.DataSource;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.lang.Nullable;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.Sid;

public class JdbcMutableAclService extends org.springframework.security.acls.jdbc.JdbcMutableAclService implements MutableAclService {

    private static final String DELETE_SID_QUERY = "delete from acl_sid where id=?";
    private static final String UPDATE_OWNER_QUERY = "update acl_object_identity set owner_sid = ? where owner_sid = ?";
    private static final String DELETE_ENTRIES_BY_SID_QUERY = "delete from acl_entry where sid=?";
    private static final String UPDATE_SID_QUERY = "update acl_sid set sid = ? where sid = ? and principal=?";
    private static final String SELECT_SID_PRIMARY_KEY = "select id from acl_sid where principal=? and sid=?";

    private final AclCache aclCache;
    private final Sid relevantSystemWideSid;

    public JdbcMutableAclService(DataSource dataSource,
                                 LookupStrategy lookupStrategy,
                                 AclCache aclCache,
                                 Sid relevantSystemWideSid) {
        super(dataSource, lookupStrategy, aclCache);
        this.aclCache = aclCache;
        this.relevantSystemWideSid = relevantSystemWideSid;
    }

    /**
     * Overrides Spring's default implementation to handle concurrent SID creation.
     * <p>
     * The parent implementation uses a non-atomic check-then-act pattern (SELECT, then INSERT),
     * which causes {@link DuplicateKeyException} when multiple threads concurrently grant
     * permissions for the same user/group on different projects. This override catches the
     * duplicate key and falls back to a SELECT, since the row now exists from the other thread.
     */
    @Override
    protected @Nullable Long createOrRetrieveSidPrimaryKey(String sidName, boolean sidIsPrincipal,
            boolean allowCreate) {
        try {
            return super.createOrRetrieveSidPrimaryKey(sidName, sidIsPrincipal, allowCreate);
        } catch (DuplicateKeyException e) {
            // Another thread inserted the same SID concurrently — retrieve it
            if (!allowCreate) {
                throw e;
            }
            return super.createOrRetrieveSidPrimaryKey(sidName, sidIsPrincipal, false);
        }
    }

    public void deleteSid(Sid sid) {
        Long sidId = createOrRetrieveSidPrimaryKey(sid, false);
        if (sidId == null) {
            return;
        }
        jdbcOperations.update(DELETE_ENTRIES_BY_SID_QUERY, sidId);

        Long newOwnerSid = createOrRetrieveSidPrimaryKey(relevantSystemWideSid, true);
        jdbcOperations.update(UPDATE_OWNER_QUERY, newOwnerSid, sidId);
        jdbcOperations.update(DELETE_SID_QUERY, sidId);
        aclCache.clearCache();
    }

    public void updateSid(Sid sid, String newSidName) {
        String currentSidName;
        boolean isPrincipal;
        if (sid instanceof GrantedAuthoritySid) {
            currentSidName = ((GrantedAuthoritySid) sid).getGrantedAuthority();
            isPrincipal = false;
        } else if (sid instanceof PrincipalSid) {
            currentSidName = ((PrincipalSid) sid).getPrincipal();
            isPrincipal = true;
        } else {
            throw new IllegalStateException("Sid type is not supported");
        }

        jdbcOperations.update(UPDATE_SID_QUERY, newSidName, currentSidName, isPrincipal);
        aclCache.clearCache();
    }
}
