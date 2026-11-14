package org.openl.rules.webstudio.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;

/**
 * @author Yury Molchan
 */
class TablePropertiesSelectorTest {

    @Test
    void severalTagsFindTablesTaggedWithAnyOfThem() {
        var selector = new TablePropertiesSelector(Map.of("tags", new String[]{"tania", "tu"}));

        assertTrue(selector.test(table(Map.of("tags", new String[]{"tania"}))));
        assertTrue(selector.test(table(Map.of("tags", new String[]{"tu"}))));
        assertTrue(selector.test(table(Map.of("tags", new String[]{"tu", "tania"}))));
        assertFalse(selector.test(table(Map.of("tags", new String[]{"other"}))));
    }

    @Test
    void everySearchedPropertyMustMatch() {
        var selector = new TablePropertiesSelector(Map.of("tags", new String[]{"tania"}, "lob", "health"));

        assertTrue(selector.test(table(Map.of("tags", new String[]{"tania"}, "lob", "Health"))));
        assertFalse(selector.test(table(Map.of("tags", new String[]{"tania"}, "lob", "auto"))));
        assertFalse(selector.test(table(Map.of("tags", new String[]{"tania"}))));
    }

    @Test
    void tableWithoutPropertiesIsNotFound() {
        var selector = new TablePropertiesSelector(Map.of("tags", new String[]{"tania"}));

        assertFalse(selector.test(mock(TableSyntaxNode.class)));
    }

    private static TableSyntaxNode table(Map<String, Object> values) {
        var properties = mock(ITableProperties.class);
        values.forEach((name, value) -> when(properties.getPropertyValue(name)).thenReturn(value));
        var node = mock(TableSyntaxNode.class);
        when(node.getTableProperties()).thenReturn(properties);
        return node;
    }
}
