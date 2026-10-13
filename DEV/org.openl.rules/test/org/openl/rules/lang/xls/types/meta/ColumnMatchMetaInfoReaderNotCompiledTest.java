package org.openl.rules.lang.xls.types.meta;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.runtime.RulesEngineFactory;

class ColumnMatchMetaInfoReaderNotCompiledTest {

    @Test
    void aTableThatDoesNotCompileHasNoBodyMetaInfo() {
        var compiled = new RulesEngineFactory<>("test/rules/cmatch1/match3-3.xls").getCompiledOpenClass();
        var tables = ((XlsMetaInfo) compiled.getOpenClassWithErrors().getMetaInfo()).getXlsModuleNode()
                .getXlsTableSyntaxNodes();
        var table = Arrays.stream(tables)
                .filter(tsn -> tsn.getMetaInfoReader() instanceof ColumnMatchMetaInfoReader)
                .findFirst()
                .orElseThrow();
        var reader = (ColumnMatchMetaInfoReader) table.getMetaInfoReader();
        var grid = table.getGridTable();

        for (var row = 1; row < grid.getHeight(); row++) {
            for (var column = 0; column < grid.getWidth(); column++) {
                var cell = grid.getCell(column, row);
                assertNull(reader.getBodyMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn()));
            }
        }
    }
}
