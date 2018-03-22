package org.openl.rules.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DataTableBindHelperTest {

    @Test
    public void collectionAccessByIndexPatternTest() {
        assertTrue("a[0]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertTrue("  a[0]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertTrue("a[0]:b  ".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertTrue("a[0]:  b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertTrue("a[0]  :b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertTrue("a[0  ]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertTrue("a[  0]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertTrue("a  [0]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));

        assertTrue("a[0]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertTrue("  a[0]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertTrue("a[0]  ".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertTrue("a[0  ]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertTrue("a[  0]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertTrue("a  [0]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));

        assertFalse("a[\"b\"]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a[  \"b\"]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a[\"b\"  ]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));

        assertFalse("a[\"b]\"]]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a[\"b\"a  ]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a[\"b\" a]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a[\"b\" 0]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a[\"b\"0 ]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));

        assertFalse("a".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a[b]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a[0]:".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a[0]:b c".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a:b[0]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a b[0]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a[b[0]]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a[0]:c:".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
        assertFalse("a[0]:c:d".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_INDEX_PATTERN));
    }

    @Test
    public void thisArrayAccessByIndexPatternTest() {
        assertTrue("[0]".matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN));
        assertTrue("  [0]".matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN));
        assertTrue("[  0]".matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN));
        assertTrue("[0  ]".matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN));
        assertTrue("[0]  ".matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN));
        assertTrue("[  0  ]".matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN));

        assertFalse("this[0]".matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN));
        assertFalse("[]".matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN));
        assertFalse("[a]  ".matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN));
        assertFalse("a[\"k\"]".matches(DataTableBindHelper.THIS_ARRAY_ACCESS_PATTERN));
    }

    @Test
    public void thisListAccessByIndexPatternTest() {
        assertTrue("[0]".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertTrue("  [0]".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertTrue("[  0]".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertTrue("[0  ]".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertTrue("[0]  ".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertTrue("[  0  ]".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertTrue("[0]:a".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertTrue("[0]  :a".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertTrue("[0]:  a".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertTrue("[0]:a  ".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));

        assertFalse("this[0]".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertFalse("[]".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertFalse("[a]  ".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertFalse("a[\"k\"]".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));

        assertFalse("[0]:a:b".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertFalse("[0]:".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
        assertFalse("[0]a".matches(DataTableBindHelper.THIS_LIST_ACCESS_PATTERN));
    }

    @Test
    public void thisMapAccessByIndexPatternTest() {
        assertTrue("[\"k\"]:b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("  [\"k\"]:b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[\"k\"]:b  ".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[\"k\"]:  b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[\"k\"]  :b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[\"k\"  ]:b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[  \"k\"]:b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("  [\"k\"]:b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));

        assertTrue("[0]:b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("  [0]:b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[0]:b  ".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[0]:  b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[0]  :b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[0  ]:b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[  0]:b".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));

        assertTrue("[0]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("  [0]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[0]  ".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[0  ]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[  0]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));

        assertTrue("[\"k\"]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("  [\"k\"]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[\"k\"]  ".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[\"k\"  ]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[  \"k\"]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));

        assertTrue("[\"k\"]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[\"k:\"]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[  \"k\"]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertTrue("[\"k\"  ]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));

        assertFalse("[\"k]\"]]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse("[\"k\"a  ]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse("[\"k\" a]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse("[\"k\" 0]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse("[\"k\"0 ]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));

        assertFalse("".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse("[b]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse("[\"k\"]:".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse("[\"k\"]:b c".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse(":b[\"k\"]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse(" b[\"k\"]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse("[b[0]]".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse("[\"k\"]:c:".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse("[\"k\"]:c:d".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
        assertFalse("[\"k\":]:c:d".matches(DataTableBindHelper.THIS_MAP_ACCESS_PATTERN));
    }
    
    @Test
    public void collectionAccessByKeyPatternTest() {
        assertTrue("a[\"k\"]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("  a[\"k\"]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[\"k\"]:b  ".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[\"k\"]:  b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[\"k\"]  :b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[\"k\"  ]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[  \"k\"]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a  [\"k\"]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));

        assertTrue("a[0]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("  a[0]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[0]:b  ".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[0]:  b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[0]  :b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[0  ]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[  0]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a  [0]:b".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));

        assertTrue("a[0]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("  a[0]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[0]  ".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[0  ]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[  0]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a  [0]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));

        assertTrue("a[\"k\"]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("  a[\"k\"]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[\"k\"]  ".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[\"k\"  ]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[  \"k\"]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a  [\"k\"]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));

        assertTrue("a[\"k\"]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[\"k:\"]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[  \"k\"]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertTrue("a[\"k\"  ]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));

        assertFalse("a[\"k]\"]]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a[\"k\"a  ]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a[\"k\" a]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a[\"k\" 0]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a[\"k\"0 ]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));

        assertFalse("a".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a[b]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a[\"k\"]:".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a[\"k\"]:b c".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a:b[\"k\"]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a b[\"k\"]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a[b[0]]".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a[\"k\"]:c:".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a[\"k\"]:c:d".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
        assertFalse("a[\"k\":]:c:d".matches(DataTableBindHelper.COLLECTION_ACCESS_BY_KEY_PATTERN));
    }

}
