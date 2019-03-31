package org.openl.rules.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openl.util.StringUtils.matches;

import org.junit.Test;

public class DataTableBindHelperTest {

    @Test
    public void collectionAccessByIndexPatternTest() {
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[0]:b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "  a[0]:b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[0]:b  "));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[0]:  b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[0]  :b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[0  ]:b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[  0]:b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a  [0]:b"));

        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[0]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "  a[0]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[0]  "));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[0  ]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[  0]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a  [0]"));

        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[\"b\"]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[  \"b\"]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[\"b\"  ]"));

        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[\"b]\"]]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[\"b\"a  ]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[\"b\" a]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[\"b\" 0]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[\"b\"0 ]"));

        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[b]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[0]:"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[0]:b c"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a:b[0]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a b[0]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[b[0]]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[0]:c:"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN, "a[0]:c:d"));
    }

    @Test
    public void thisArrayAccessByIndexPatternTest() {
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
    public void thisListAccessByIndexPatternTest() {
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

    @Test
    public void thisMapAccessByIndexPatternTest() {
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"]:b"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "  [\"k\"]:b"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"]:b  "));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"]:  b"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"]  :b"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"  ]:b"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[  \"k\"]:b"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "  [\"k\"]:b"));

        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[0]:b"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "  [0]:b"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[0]:b  "));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[0]:  b"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[0]  :b"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[0  ]:b"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[  0]:b"));

        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[0]"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "  [0]"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[0]  "));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[0  ]"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[  0]"));

        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"]"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "  [\"k\"]"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"]  "));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"  ]"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[  \"k\"]"));

        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"]"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k:\"]"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[  \"k\"]"));
        assertTrue(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"  ]"));

        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k]\"]]"));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"a  ]"));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\" a]"));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\" 0]"));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"0 ]"));

        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, ""));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[b]"));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"]:"));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"]:b c"));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, ":b[\"k\"]"));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, " b[\"k\"]"));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[b[0]]"));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"]:c:"));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\"]:c:d"));
        assertFalse(matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN, "[\"k\":]:c:d"));
    }

    @Test
    public void collectionAccessByKeyPatternTest() {
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"]:b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "  a[\"k\"]:b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"]:b  "));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"]:  b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"]  :b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"  ]:b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[  \"k\"]:b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a  [\"k\"]:b"));

        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[0]:b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "  a[0]:b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[0]:b  "));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[0]:  b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[0]  :b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[0  ]:b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[  0]:b"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a  [0]:b"));

        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[0]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "  a[0]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[0]  "));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[0  ]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[  0]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a  [0]"));

        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "  a[\"k\"]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"]  "));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"  ]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[  \"k\"]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a  [\"k\"]"));

        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k:\"]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[  \"k\"]"));
        assertTrue(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"  ]"));

        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k]\"]]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"a  ]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\" a]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\" 0]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"0 ]"));

        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[b]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"]:"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"]:b c"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a:b[\"k\"]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a b[\"k\"]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[b[0]]"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"]:c:"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\"]:c:d"));
        assertFalse(matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN, "a[\"k\":]:c:d"));
    }

}
