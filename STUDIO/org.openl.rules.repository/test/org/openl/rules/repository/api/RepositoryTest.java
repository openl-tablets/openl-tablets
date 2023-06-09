package org.openl.rules.repository.api;

import java.nio.file.InvalidPathException;

import org.junit.Assert;
import org.junit.Test;

public class RepositoryTest {

    @Test
    public void validPath() {
        Repository.validatePath(null);
        Repository.validatePath("");
        Repository.validatePath(" ");
        Repository.validatePath("a");
        Repository.validatePath("!");
        Repository.validatePath("path");
        Repository.validatePath("valid/path");
        Repository.validatePath("allowed/path.");
        Repository.validatePath("allowed/path..");
        Repository.validatePath("allowed/path...");
        Repository.validatePath("allowed/path.../");
        Repository.validatePath("allowed/path../");
        Repository.validatePath("allowed/path./");
        Repository.validatePath(" a white spaces ");
    }

    @Test
    public void invalidPath() {
        assertInvalidPath("/absolute");
        assertInvalidPath("/");
        assertInvalidPath(".");
        assertInvalidPath("..");
        assertInvalidPath("./path");
        assertInvalidPath("../path");
        assertInvalidPath("path/../");
        assertInvalidPath("invalid/../path");
        assertInvalidPath("invalid/./path");
        assertInvalidPath("path/..");
        assertInvalidPath("path/.");
        assertInvalidPath("invalid\\path");
        assertInvalidPath("invalid//path");
        assertInvalidPath("\\invalid");
        assertInvalidPath("invalid\\");

    }

    private static void assertInvalidPath(String path) {
        try {
            Repository.validatePath(path);
            Assert.fail();
        } catch (InvalidPathException ex) {
            // pass
        }
    }
}
