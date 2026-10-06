package org.openl.rules.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;

/**
 * A reference into a Data table whose first column, its key, has no field. Such a column holds no keys, so the
 * reference finds none and offers none to choose from.
 */
class ReferenceToKeyColumnWithoutFieldTest {

    private static final String WORKBOOK =
            "test-resources/functionality/DataTables_ReferenceToKeyColumnWithoutField.xlsx";

    @Test
    void offersNoKeysToChooseFrom() throws Exception {
        var module = (XlsModuleOpenClass) new RulesEngineFactory<>(WORKBOOK).getCompiledOpenClass()
                .getOpenClassWithErrors();
        var dataBase = module.getDataBase();
        var driver = Arrays.stream(dataBase.getTable("policies").getDataModel().getDescriptors())
                .filter(ForeignKeyColumnDescriptor.class::isInstance)
                .map(ForeignKeyColumnDescriptor.class::cast)
                .findFirst()
                .orElseThrow();

        assertNotNull(dataBase.getTable("drivers"));
        assertNull(driver.getDomainClassForForeignTable(dataBase));
    }
}
