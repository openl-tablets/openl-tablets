package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class WildcardBranchNameFilterTest {

    @Test
    public void testBranchMatcherNegative() {
        Exception actualEx = assertThrows(Exception.class, () -> {
            String[] ar = null;
            new WildcardBranchNameFilterImpl(ar);
        });
        assertEquals("Branch name pattern list cannot be null.", actualEx.getMessage());
    }

    @Test
    public void testBranchMatcher() {
        WildcardBranchNameFilter branchNameFilter = WildcardBranchNameFilter.create("master", null, "release-*");
        assertTrue(branchNameFilter.test("master"));
        assertTrue(branchNameFilter.test("release-21.1"));
        assertTrue(branchNameFilter.test("release-21"));

        assertFalse(branchNameFilter.test("EPBDS-11646"));

        branchNameFilter = WildcardBranchNameFilter.create(null, null);
        assertFalse(branchNameFilter.test("master"));
        assertFalse(branchNameFilter.test("release-21.1"));
        assertFalse(branchNameFilter.test("EPBDS-11646"));

        branchNameFilter = WildcardBranchNameFilter.create("*");
        assertTrue(branchNameFilter.test("master"));
        assertTrue(branchNameFilter.test("release-21.1"));
        assertTrue(branchNameFilter.test("EPBDS-11646"));
        assertFalse(branchNameFilter.test("release/21.1"));

        branchNameFilter = WildcardBranchNameFilter.create("**");
        assertTrue(branchNameFilter.test("master"));
        assertTrue(branchNameFilter.test("release-21.1"));
        assertTrue(branchNameFilter.test("EPBDS-11646"));
        assertTrue(branchNameFilter.test("release/21.1"));

        branchNameFilter = WildcardBranchNameFilter.create("**.*");
        assertFalse(branchNameFilter.test("master"));
        assertTrue(branchNameFilter.test("release-21.1"));
        assertFalse(branchNameFilter.test("EPBDS-11646"));
        assertTrue(branchNameFilter.test("release/21.1"));
    }

}
