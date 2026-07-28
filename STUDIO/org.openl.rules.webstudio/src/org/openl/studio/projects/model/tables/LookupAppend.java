package org.openl.studio.projects.model.tables;

import java.util.LinkedHashMap;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Request model for appending rows to SmartLookup table
 *
 * @author Vladyslav Pikus
 */
public class LookupAppend implements AppendTableView {

    @Getter
    @Schema(description = "Type of lookup table (SmartLookup or SimpleLookup)")
    @Setter
    public String tableType;

    @Getter
    @Schema(description = "Data rows with hierarchical structure to append")
    @Setter
    private List<LinkedHashMap<String, Object>> rows;
}
