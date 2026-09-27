package org.openl.rules.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import static org.openl.util.StringUtils.matches;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DataTableBindHelperTest {

    static Stream<Arguments> collectionAccessByIndexPatternTest() {
        return Stream.of(
                arguments("a[0]:b", true),
                arguments("  a[0]:b", true),
                arguments("a[0]:b  ", true),
                arguments("a[0]:  b", true),
                arguments("a[0]  :b", true),
                arguments("a[0  ]:b", true),
                arguments("a[  0]:b", true),
                arguments("a  [0]:b", true),

                arguments("a[0]", true),
                arguments("  a[0]", true),
                arguments("a[0]  ", true),
                arguments("a[0  ]", true),
                arguments("a[  0]", true),
                arguments("a  [0]", true),

                arguments("a[\"b\"]", false),
                arguments("a[  \"b\"]", false),
                arguments("a[\"b\"  ]", false),

                arguments("a[\"b]\"]]", false),
                arguments("a[\"b\"a  ]", false),
                arguments("a[\"b\" a]", false),
                arguments("a[\"b\" 0]", false),
                arguments("a[\"b\"0 ]", false),

                arguments("a", false),
                arguments("a[b]", false),
                arguments("a[0]:", false),
                arguments("a[0]:b c", false),
                arguments("a:b[0]", false),
                arguments("a b[0]", false),
                arguments("a[b[0]]", false),
                arguments("a[0]:c:", false),
                arguments("a[0]:c:d", false));
    }

    @ParameterizedTest
    @MethodSource
    void collectionAccessByIndexPatternTest(String text, boolean expected) {
        assertEquals(expected, matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, text));
    }

    @Test
    void thisArrayAccessByIndexPatternTest() {
        assertTrue(matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN, "[0]"));
        assertTrue(matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN, "  [0]"));
        assertTrue(matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN, "[  0]"));
        assertTrue(matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN, "[0  ]"));
        assertTrue(matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN, "[0]  "));
        assertTrue(matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN, "[  0  ]"));

        assertFalse(matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN, "this[0]"));
        assertFalse(matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN, "[]"));
        assertFalse(matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN, "[a]  "));
        assertFalse(matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN, "a[\"k\"]"));
    }

    @Test
    void thisListAccessByIndexPatternTest() {
        assertTrue(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[0]"));
        assertTrue(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "  [0]"));
        assertTrue(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[  0]"));
        assertTrue(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[0  ]"));
        assertTrue(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[0]  "));
        assertTrue(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[  0  ]"));
        assertTrue(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[0]:a"));
        assertTrue(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[0]  :a"));
        assertTrue(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[0]:  a"));
        assertTrue(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[0]:a  "));

        assertFalse(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "this[0]"));
        assertFalse(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[]"));
        assertFalse(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[a]  "));
        assertFalse(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "a[\"k\"]"));

        assertFalse(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[0]:a:b"));
        assertFalse(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[0]:"));
        assertFalse(matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN, "[0]a"));
    }

    static Stream<Arguments> thisMapAccessByIndexPatternTest() {
        return Stream.of(
                arguments("[\"k\"]:b", true),
                arguments("  [\"k\"]:b", true),
                arguments("[\"k\"]:b  ", true),
                arguments("[\"k\"]:  b", true),
                arguments("[\"k\"]  :b", true),
                arguments("[\"k\"  ]:b", true),
                arguments("[  \"k\"]:b", true),
                arguments("  [\"k\"]:b", true),

                arguments("[0]:b", true),
                arguments("  [0]:b", true),
                arguments("[0]:b  ", true),
                arguments("[0]:  b", true),
                arguments("[0]  :b", true),
                arguments("[0  ]:b", true),
                arguments("[  0]:b", true),

                arguments("[0]", true),
                arguments("  [0]", true),
                arguments("[0]  ", true),
                arguments("[0  ]", true),
                arguments("[  0]", true),

                arguments("[\"k\"]", true),
                arguments("  [\"k\"]", true),
                arguments("[\"k\"]  ", true),
                arguments("[\"k\"  ]", true),
                arguments("[  \"k\"]", true),

                arguments("[\"k\"]", true),
                arguments("[\"k:\"]", true),
                arguments("[  \"k\"]", true),
                arguments("[\"k\"  ]", true),

                arguments("[\"k]\"]]", false),
                arguments("[\"k\"a  ]", false),
                arguments("[\"k\" a]", false),
                arguments("[\"k\" 0]", false),
                arguments("[\"k\"0 ]", false),

                arguments("", false),
                arguments("[b]", false),
                arguments("[\"k\"]:", false),
                arguments("[\"k\"]:b c", false),
                arguments(":b[\"k\"]", false),
                arguments(" b[\"k\"]", false),
                arguments("[b[0]]", false),
                arguments("[\"k\"]:c:", false),
                arguments("[\"k\"]:c:d", false),
                arguments("[\"k\":]:c:d", false));
    }

    @ParameterizedTest
    @MethodSource
    void thisMapAccessByIndexPatternTest(String text, boolean expected) {
        assertEquals(expected, matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, text));
    }

    static Stream<Arguments> collectionAccessByKeyPatternTest() {
        return Stream.of(
                arguments("a[\"k\"]:b", true),
                arguments("  a[\"k\"]:b", true),
                arguments("a[\"k\"]:b  ", true),
                arguments("a[\"k\"]:  b", true),
                arguments("a[\"k\"]  :b", true),
                arguments("a[\"k\"  ]:b", true),
                arguments("a[  \"k\"]:b", true),
                arguments("a  [\"k\"]:b", true),

                arguments("a[0]:b", true),
                arguments("  a[0]:b", true),
                arguments("a[0]:b  ", true),
                arguments("a[0]:  b", true),
                arguments("a[0]  :b", true),
                arguments("a[0  ]:b", true),
                arguments("a[  0]:b", true),
                arguments("a  [0]:b", true),

                arguments("a[0]", true),
                arguments("  a[0]", true),
                arguments("a[0]  ", true),
                arguments("a[0  ]", true),
                arguments("a[  0]", true),
                arguments("a  [0]", true),

                arguments("a[\"k\"]", true),
                arguments("  a[\"k\"]", true),
                arguments("a[\"k\"]  ", true),
                arguments("a[\"k\"  ]", true),
                arguments("a[  \"k\"]", true),
                arguments("a  [\"k\"]", true),

                arguments("a[\"k\"]", true),
                arguments("a[\"k:\"]", true),
                arguments("a[  \"k\"]", true),
                arguments("a[\"k\"  ]", true),

                arguments("a[\"k]\"]]", false),
                arguments("a[\"k\"a  ]", false),
                arguments("a[\"k\" a]", false),
                arguments("a[\"k\" 0]", false),
                arguments("a[\"k\"0 ]", false),

                arguments("a", false),
                arguments("a[b]", false),
                arguments("a[\"k\"]:", false),
                arguments("a[\"k\"]:b c", false),
                arguments("a:b[\"k\"]", false),
                arguments("a b[\"k\"]", false),
                arguments("a[b[0]]", false),
                arguments("a[\"k\"]:c:", false),
                arguments("a[\"k\"]:c:d", false),
                arguments("a[\"k\":]:c:d", false));
    }

    @ParameterizedTest
    @MethodSource
    void collectionAccessByKeyPatternTest(String text, boolean expected) {
        assertEquals(expected, matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, text));
    }

}
