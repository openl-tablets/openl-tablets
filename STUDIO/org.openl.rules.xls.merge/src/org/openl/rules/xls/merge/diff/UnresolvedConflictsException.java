package org.openl.rules.xls.merge.diff;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

/**
 * Thrown when a workbook cannot be merged as decided: a conflict has no resolution, a table resolved cell by cell
 * cannot be merged so, or the resolutions chosen for a sheet leave it a conflict.
 */
@Getter
public class UnresolvedConflictsException extends Exception {

    /**
     * The identifiers of the conflicts that have no resolution, a table with a conflicting cell left without a
     * resolution included
     */
    private final List<String> unresolved;
    /**
     * The identifiers of the tables resolved cell by cell that cannot be merged so; each is taken whole from one
     * revision
     */
    private final List<String> notByCells;
    /**
     * The sheets that cannot be merged as decided, with the reasons, by their names in the order of the workbook; each
     * is resolved only as a whole
     */
    private final Map<String, SheetConflictReason> refused;

    public UnresolvedConflictsException(List<String> unresolved,
                                        List<String> notByCells,
                                        Map<String, SheetConflictReason> refused) {
        super("Conflicts left unresolved: " + unresolved + ", tables not merged cell by cell: " + notByCells
                + ", sheets refused: " + refused.keySet());
        this.unresolved = List.copyOf(unresolved);
        this.notByCells = List.copyOf(notByCells);
        this.refused = Collections.unmodifiableMap(new LinkedHashMap<>(refused));
    }
}
