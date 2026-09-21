package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.TableKind;

class OpenLTableUtilsTest {

    @Test
    void namesTheFamilyOfATableByItsType() {
        assertEquals(TableKind.RULES, OpenLTableUtils.kindOf(table(XlsNodeTypes.XLS_DT)));
        assertEquals(TableKind.DATATYPE, OpenLTableUtils.kindOf(table(XlsNodeTypes.XLS_DATATYPE)));
    }

    @Test
    void putsATypeOfNoFamilyOfItsOwnUnderOther() {
        // A part of a table split across sheets is a table the module lists, but no family the dictionary names.
        assertEquals(TableKind.OTHER, OpenLTableUtils.kindOf(table(XlsNodeTypes.XLS_TABLEPART)));
    }

    private static IOpenLTable table(XlsNodeTypes type) {
        var table = mock(IOpenLTable.class);
        when(table.getType()).thenReturn(type.toString());
        return table;
    }
}
