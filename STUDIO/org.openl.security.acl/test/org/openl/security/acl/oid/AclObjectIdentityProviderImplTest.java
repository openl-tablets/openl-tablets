package org.openl.security.acl.oid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.ObjectIdentity;

import org.openl.security.acl.repository.ProjectArtifact;
import org.openl.security.acl.repository.Root;

public class AclObjectIdentityProviderImplTest {

    private static final String ROOT_ID = "1";

    private AclObjectIdentityProvider oidProvider;

    @BeforeEach
    public void setUp() {
        oidProvider = new AclObjectIdentityProviderImpl(ProjectArtifact.class, ROOT_ID);
    }

    @Test
    public void getRootOidTest() {
        ObjectIdentity oid = oidProvider.getRootOid();
        assertNotNull(oid);
        assertEquals(ROOT_ID, oid.getIdentifier());
        assertEquals(Root.class.getName(), oid.getType());
    }

    @Test
    public void getRepositoryOidTest() {
        ObjectIdentity oid = oidProvider.getRepositoryOid("repoId", "/path1/path2/file.xlsx");
        assertNotNull(oid);
        assertEquals("repoId:/path1/path2/file.xlsx", oid.getIdentifier());
        assertEquals(ProjectArtifact.class.getName(), oid.getType());
    }

    @Test
    public void getParentOidTest() {
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, "repoId:/path1/path2/file.xlsx");
        ObjectIdentity poi = oidProvider.getParentOid(oi);
        assertNotNull(poi);
        assertEquals("repoId:/path1/path2", poi.getIdentifier());
        assertEquals(ProjectArtifact.class.getName(), poi.getType());

        oi = poi;
        poi = oidProvider.getParentOid(oi);
        assertNotNull(poi);
        assertEquals("repoId:/path1", poi.getIdentifier());
        assertEquals(ProjectArtifact.class.getName(), poi.getType());

        oi = poi;
        poi = oidProvider.getParentOid(oi);
        assertNotNull(poi);
        assertEquals("repoId", poi.getIdentifier());
        assertEquals(ProjectArtifact.class.getName(), poi.getType());

        oi = poi;
        poi = oidProvider.getParentOid(oi);
        assertNotNull(poi);
        assertEquals(ROOT_ID, poi.getIdentifier());
        assertEquals(Root.class.getName(), poi.getType());

        oi = poi;
        poi = oidProvider.getParentOid(oi);
        assertNull(poi);
    }

    @Test
    public void moveToNewParentTest() {
        ObjectIdentity childOid = new ObjectIdentityImpl(ProjectArtifact.class, "repoId:/path1/path2/file.xlsx");
        ObjectIdentity oldParentOid = new ObjectIdentityImpl(ProjectArtifact.class, "repoId:/path1");
        ObjectIdentity newParentOid = new ObjectIdentityImpl(ProjectArtifact.class, "repoId:/newPath");

        ObjectIdentity movedOid = oidProvider.moveToNewParent(childOid, oldParentOid, newParentOid);
        assertNotNull(movedOid);
        assertEquals("repoId:/newPath/path2/file.xlsx", movedOid.getIdentifier());

        childOid = new ObjectIdentityImpl(ProjectArtifact.class, "repoId:/path1");
        movedOid = oidProvider.moveToNewParent(childOid, oldParentOid, newParentOid);
        assertNotNull(movedOid);
        assertEquals("repoId:/newPath", movedOid.getIdentifier());
    }

}
