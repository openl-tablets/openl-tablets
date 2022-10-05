package org.openl.security.acl.repository;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.ObjectIdentity;

public class DesignRepositoryAclServiceImplTest {
    @Test
    public void buildParentObjectIdentity() {
        ObjectIdentity oi = new ObjectIdentityImpl(ProjectArtifact.class, "repoId:/path1/path2/file.xlsx");
        ObjectIdentity poi = DesignRepositoryAclServiceImpl.buildParentObjectIdentity(oi);
        Assert.assertEquals("repoId:/path1/path2", poi.getIdentifier());

        oi = poi;
        poi = DesignRepositoryAclServiceImpl.buildParentObjectIdentity(oi);
        Assert.assertEquals("repoId:/path1", poi.getIdentifier());

        oi = poi;
        poi = DesignRepositoryAclServiceImpl.buildParentObjectIdentity(oi);
        Assert.assertEquals("repoId", poi.getIdentifier());
    }

    @Test
    public void concat() {
        Assert.assertEquals("repo1", DesignRepositoryAclServiceImpl.concat("repo1", ""));
        Assert.assertEquals("repo1", DesignRepositoryAclServiceImpl.concat("repo1", "/"));
        Assert.assertEquals("repo1:/path1", DesignRepositoryAclServiceImpl.concat("repo1", "/path1"));
        Assert.assertEquals("repo1:/path1", DesignRepositoryAclServiceImpl.concat("repo1", "/path1/"));
        Assert.assertEquals("repo1:/path1/path2", DesignRepositoryAclServiceImpl.concat("repo1", "/path1/path2"));
    }
}
