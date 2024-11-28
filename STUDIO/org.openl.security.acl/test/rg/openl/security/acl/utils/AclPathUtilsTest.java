package rg.openl.security.acl.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.security.acl.utils.AclPathUtils;

public class AclPathUtilsTest {

    @Test
    public void concatTest() {
        assertEquals("repo1", AclPathUtils.buildRepositoryPath("repo1", null));
        assertEquals("repo1", AclPathUtils.buildRepositoryPath("repo1", ""));
        assertEquals("repo1", AclPathUtils.buildRepositoryPath("repo1", "/"));
        assertEquals("repo1:/path1", AclPathUtils.buildRepositoryPath("repo1", "/path1"));
        assertEquals("repo1:/path1", AclPathUtils.buildRepositoryPath("repo1", "/path1/"));
        assertEquals("repo1:/path1/path2", AclPathUtils.buildRepositoryPath("repo1", "/path1/path2"));
        assertEquals("repo1:/path1/path2", AclPathUtils.buildRepositoryPath("repo1", "//path1////path2//"));
        assertEquals("repo1:/path1/path2", AclPathUtils.buildRepositoryPath("repo1", "//path1/    /path2//"));
    }

}
