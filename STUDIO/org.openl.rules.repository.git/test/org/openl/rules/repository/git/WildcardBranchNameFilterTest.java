package org.openl.rules.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WildcardBranchNameFilterTest {

    @Test
    void testBranchMatcherNegative() {
        Exception actualEx = assertThrows(Exception.class, () -> {
            String[] ar = null;
            new WildcardBranchNameFilterImpl(ar);
        });
        assertEquals("Branch name pattern list cannot be null.", actualEx.getMessage());
    }

    static Stream<Arguments> testBranchMatcher() {
        return Stream.of(
                arguments(new String[]{"master", null, "release-*"}, "master", true),
                arguments(new String[]{"master", null, "release-*"}, "release-21.1", true),
                arguments(new String[]{"master", null, "release-*"}, "release-21", true),

                arguments(new String[]{"master", null, "release-*"}, "EPBDS-11646", false),

                arguments(new String[]{null, null}, "master", false),
                arguments(new String[]{null, null}, "release-21.1", false),
                arguments(new String[]{null, null}, "EPBDS-11646", false),

                arguments(new String[]{"*"}, "master", true),
                arguments(new String[]{"*"}, "release-21.1", true),
                arguments(new String[]{"*"}, "EPBDS-11646", true),
                arguments(new String[]{"*"}, "release/21.1", false),

                arguments(new String[]{"**"}, "master", true),
                arguments(new String[]{"**"}, "release-21.1", true),
                arguments(new String[]{"**"}, "EPBDS-11646", true),
                arguments(new String[]{"**"}, "release/21.1", true),

                arguments(new String[]{"**.*"}, "master", false),
                arguments(new String[]{"**.*"}, "release-21.1", true),
                arguments(new String[]{"**.*"}, "EPBDS-11646", false),
                arguments(new String[]{"**.*"}, "release/21.1", true));
    }

    @ParameterizedTest
    @MethodSource
    void testBranchMatcher(String[] patterns, String branch, boolean expected) {
        assertEquals(expected, WildcardBranchNameFilter.create(patterns).test(branch));
    }

}
