package org.openl.studio.projects.model.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Verifies the nested request shape: {@code operation} selects the action and the {@code target} object's
 * {@code type} selects the resource it acts on. Binding uses standard Jackson polymorphism (no custom deserializer).
 */
class RawTableSourceActionTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializesAppendRowsWithInlineMerge() throws Exception {
        var action = read("{\"operation\":\"append\",\"target\":{\"type\":\"rows\","
                + "\"cells\":[[{\"value\":\"x\"},{\"value\":2,\"colspan\":2}]]}}");
        var append = assertInstanceOf(RawTableSourceAction.Append.class, action);
        var rows = assertInstanceOf(AppendTarget.Rows.class, append.target());
        assertEquals(1, rows.cells().size());
        assertEquals(2, rows.cells().get(0).size());
        assertEquals("x", rows.cells().get(0).get(0).value());
        assertEquals(2, rows.cells().get(0).get(1).colspan());
    }

    @Test
    void deserializesInsertColumns() throws Exception {
        var action = read("{\"operation\":\"insert\",\"target\":"
                + "{\"type\":\"columns\",\"position\":3,\"cells\":[[{\"value\":\"a\"}]]}}");
        var insert = assertInstanceOf(RawTableSourceAction.Insert.class, action);
        var columns = assertInstanceOf(InsertTarget.Columns.class, insert.target());
        assertEquals(3, columns.position());
        assertEquals(1, columns.cells().size());
    }

    @Test
    void deserializesInsertRowsBlock() throws Exception {
        var action = read("{\"operation\":\"insert\",\"target\":{\"type\":\"rows\",\"position\":2,"
                + "\"cells\":[[{\"value\":\"a\"},{\"value\":\"b\"}],[{\"value\":\"c\"},{\"value\":\"d\"}]]}}");
        var insert = assertInstanceOf(RawTableSourceAction.Insert.class, action);
        var rows = assertInstanceOf(InsertTarget.Rows.class, insert.target());
        assertEquals(2, rows.position());
        assertEquals(2, rows.cells().size());
        assertEquals("a", rows.cells().get(0).get(0).value());
    }

    @Test
    void deserializesAppendRowsBlock() throws Exception {
        var action = read("{\"operation\":\"append\",\"target\":{\"type\":\"rows\","
                + "\"cells\":[[{\"value\":\"a\"}],[{\"value\":\"b\"}]]}}");
        var append = assertInstanceOf(RawTableSourceAction.Append.class, action);
        var rows = assertInstanceOf(AppendTarget.Rows.class, append.target());
        assertEquals(2, rows.cells().size());
    }

    @Test
    void deserializesDeleteRowsBlock() throws Exception {
        var action = read("{\"operation\":\"delete\",\"target\":{\"type\":\"rows\",\"position\":2,\"count\":3}}");
        var delete = assertInstanceOf(RawTableSourceAction.Delete.class, action);
        var rows = assertInstanceOf(DeleteTarget.Rows.class, delete.target());
        assertEquals(2, rows.position());
        assertEquals(3, rows.count());
    }

    @Test
    void deserializesUpdateCellIncludingNullValue() throws Exception {
        var action = read("{\"operation\":\"update\",\"target\":"
                + "{\"type\":\"cell\",\"row\":1,\"column\":2,\"value\":\"v\"}}");
        var update = assertInstanceOf(RawTableSourceAction.Update.class, action);
        var cell = assertInstanceOf(UpdateTarget.Cell.class, update.target());
        assertEquals(1, cell.row());
        assertEquals(2, cell.column());
        assertEquals("v", cell.value());

        var cleared = (RawTableSourceAction.Update) read(
                "{\"operation\":\"update\",\"target\":{\"type\":\"cell\",\"row\":0,\"column\":0,\"value\":null}}");
        assertEquals(null, ((UpdateTarget.Cell) cleared.target()).value());
    }

    @Test
    void deserializesUpdateRange() throws Exception {
        var action = read("{\"operation\":\"update\",\"target\":{\"type\":\"range\",\"row\":1,\"column\":2,"
                + "\"cells\":[[{\"value\":\"a\"},{\"value\":\"b\"}],[{\"value\":\"c\"},{\"value\":\"d\"}]]}}");
        var update = assertInstanceOf(RawTableSourceAction.Update.class, action);
        var range = assertInstanceOf(UpdateTarget.Range.class, update.target());
        assertEquals(1, range.row());
        assertEquals(2, range.column());
        assertEquals(2, range.cells().size());
        assertEquals("a", range.cells().get(0).get(0).value());
        assertEquals("d", range.cells().get(1).get(1).value());
    }

    @Test
    void deserializesMerge() throws Exception {
        var action = read("{\"operation\":\"merge\",\"target\":"
                + "{\"type\":\"cells\",\"row\":1,\"column\":2,\"rowspan\":2,\"colspan\":3}}");
        var merge = assertInstanceOf(RawTableSourceAction.Merge.class, action);
        var cells = assertInstanceOf(MergeTarget.Cells.class, merge.target());
        assertEquals(1, cells.row());
        assertEquals(2, cells.column());
        assertEquals(2, cells.rowspan());
        assertEquals(3, cells.colspan());
    }

    @Test
    void deserializesUnmerge() throws Exception {
        var action = read("{\"operation\":\"unmerge\",\"target\":{\"type\":\"cells\",\"row\":1,\"column\":0}}");
        var unmerge = assertInstanceOf(RawTableSourceAction.Unmerge.class, action);
        var cells = assertInstanceOf(UnmergeTarget.Cells.class, unmerge.target());
        assertEquals(1, cells.row());
        assertEquals(0, cells.column());
    }

    @Test
    void rejectsUnknownOperation() {
        assertThrows(JsonMappingException.class, () -> read("{\"operation\":\"flip\",\"target\":{\"type\":\"rows\"}}"));
    }

    @Test
    void rejectsUnknownType() {
        assertThrows(JsonMappingException.class,
                () -> read("{\"operation\":\"append\",\"target\":{\"type\":\"diagonal\"}}"));
    }

    private RawTableSourceAction read(String json) throws Exception {
        return mapper.readValue(json, RawTableSourceAction.class);
    }

}
