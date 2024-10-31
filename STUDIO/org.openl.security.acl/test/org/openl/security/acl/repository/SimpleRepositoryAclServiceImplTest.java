package org.openl.security.acl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.ObjectIdentity;

public class SimpleRepositoryAclServiceImplTest {

    @Test
    public void buildParentObjectIdentityTest() {
        final var rootId = "1";
        assertNull(SimpleRepositoryAclServiceImpl.buildParentObjectIdentity(new ObjectIdentityImpl(Root.class, rootId), ProjectArtifact.class, rootId));

        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, "repoId:/path1/path2/file.xlsx");
        ObjectIdentity poi = SimpleRepositoryAclServiceImpl.buildParentObjectIdentity(oi, ProjectArtifact.class, rootId);
        assertNotNull(poi);
        assertEquals("repoId:/path1/path2", poi.getIdentifier());

        oi = poi;
        poi = SimpleRepositoryAclServiceImpl.buildParentObjectIdentity(oi, ProjectArtifact.class, rootId);
        assertNotNull(poi);
        assertEquals("repoId:/path1", poi.getIdentifier());

        oi = poi;
        poi = SimpleRepositoryAclServiceImpl.buildParentObjectIdentity(oi, ProjectArtifact.class, rootId);
        assertNotNull(poi);
        assertEquals("repoId", poi.getIdentifier());

        oi = poi;
        poi = SimpleRepositoryAclServiceImpl.buildParentObjectIdentity(oi, ProjectArtifact.class, rootId);
        assertNotNull(poi);
        assertEquals(rootId, poi.getIdentifier());
    }

}
