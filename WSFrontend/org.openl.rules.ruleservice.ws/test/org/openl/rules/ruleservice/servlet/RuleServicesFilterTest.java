package org.openl.rules.ruleservice.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RuleServicesFilterTest {

    @Test
    public void isAllowedPath() {
        assertFalse(RuleServicesFilter.isAllowedPath(""));
        assertFalse(RuleServicesFilter.isAllowedPath("file"));
        assertFalse(RuleServicesFilter.isAllowedPath("file.txt"));
        assertFalse(RuleServicesFilter.isAllowedPath("path/file.txt"));
        assertFalse(RuleServicesFilter.isAllowedPath("//file"));
        assertFalse(RuleServicesFilter.isAllowedPath(".file"));
        assertFalse(RuleServicesFilter.isAllowedPath("/.file"));
        assertFalse(RuleServicesFilter.isAllowedPath("/./file"));
        assertFalse(RuleServicesFilter.isAllowedPath("/../file"));
        assertFalse(RuleServicesFilter.isAllowedPath("/file..txt"));
        assertFalse(RuleServicesFilter.isAllowedPath("/%2E/file.txt"));
        assertFalse(RuleServicesFilter.isAllowedPath("/path%2Epath/file.txt"));
        assertFalse(RuleServicesFilter.isAllowedPath("/%2E%2E/file.txt"));
        assertFalse(RuleServicesFilter.isAllowedPath("%2File.txt"));
        assertFalse(RuleServicesFilter.isAllowedPath("/%44File.txt"));

        assertTrue(RuleServicesFilter.isAllowedPath("/file"));
        assertTrue(RuleServicesFilter.isAllowedPath("/file.txt"));
        assertTrue(RuleServicesFilter.isAllowedPath("/path/file.txt"));
        assertTrue(RuleServicesFilter.isAllowedPath("/path/file.txt.gz"));
        assertTrue(RuleServicesFilter.isAllowedPath("/path/path.ext/file.txt.gz"));
    }
}
