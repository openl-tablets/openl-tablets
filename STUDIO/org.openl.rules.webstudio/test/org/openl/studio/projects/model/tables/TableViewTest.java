package org.openl.studio.projects.model.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TableViewTest {

    @Test
    void preservesPropertiesOrderForWriting() {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("state", "AL");
        properties.put("lob", "TEST");

        var table = SimpleSpreadsheetView.builder().properties(properties).build();

        assertEquals(List.of("state", "lob"), List.copyOf(table.properties.keySet()));
        assertThrows(UnsupportedOperationException.class, () -> table.properties.put("new", "value"));
    }

    @Test
    void preservesJsonPropertiesOrderForWriting() throws Exception {
        var request = new ObjectMapper().readValue("""
                {
                  "moduleName": "Main",
                  "sheetName": "TwoProperties",
                  "table": {
                    "tableType": "SimpleSpreadsheet",
                    "properties": {
                      "state": "AL",
                      "lob": "TEST"
                    }
                  }
                }
                """, CreateNewTableRequest.class);

        var table = (TableView) request.table();
        assertEquals(List.of("state", "lob"), List.copyOf(table.properties.keySet()));
    }
}
