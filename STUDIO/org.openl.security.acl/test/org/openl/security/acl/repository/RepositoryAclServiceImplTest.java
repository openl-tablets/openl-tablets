package org.openl.security.acl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.ObjectIdentity;

public class RepositoryAclServiceImplTest {
    @Test
    public void buildParentObjectIdentity() {
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, "repoId:/path1/path2/file.xlsx");
        ObjectIdentity poi = RepositoryAclServiceImpl.buildParentObjectIdentity(oi, ProjectArtifact.class, "1");
        assertEquals("repoId:/path1/path2", poi.getIdentifier());

        oi = poi;
        poi = SimpleRepositoryAclServiceImpl.buildParentObjectIdentity(oi, ProjectArtifact.class, "1");
        assertEquals("repoId:/path1", poi.getIdentifier());

        oi = poi;
        poi = SimpleRepositoryAclServiceImpl.buildParentObjectIdentity(oi, ProjectArtifact.class, "1");
        assertEquals("repoId", poi.getIdentifier());
    }

    @Test
    public void concat() {
        assertEquals("repo1", RepositoryAclServiceImpl.concat("repo1", ""));
        assertEquals("repo1", RepositoryAclServiceImpl.concat("repo1", "/"));
        assertEquals("repo1:/path1", RepositoryAclServiceImpl.concat("repo1", "/path1"));
        assertEquals("repo1:/path1", RepositoryAclServiceImpl.concat("repo1", "/path1/"));
        assertEquals("repo1:/path1/path2", RepositoryAclServiceImpl.concat("repo1", "/path1/path2"));
    }
}
