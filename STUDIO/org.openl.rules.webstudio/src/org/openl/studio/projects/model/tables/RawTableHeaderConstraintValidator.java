package org.openl.studio.projects.model.tables;

import java.util.List;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.openl.rules.lang.xls.XlsHelper;

/**
 * Rejects a raw table whose top-left cell does not name a table type OpenL recognizes.
 *
 * @author Vladyslav Pikus
 */
public class RawTableHeaderConstraintValidator implements ConstraintValidator<RawTableHeaderConstraint, RawTableView> {

    @Override
    public boolean isValid(RawTableView view, ConstraintValidatorContext context) {
        if (view == null || view.source == null || view.source.isEmpty()) {
            return true; // an empty source is already reported by @NotEmpty
        }
        List<RawTableCell> firstRow = view.source.get(0);
        if (firstRow == null || firstRow.isEmpty()) {
            return true; // an empty first row is a structural issue reported by other constraints
        }
        // A null cell or a null/blank value at the top-left is no header at all; isKnownTableHeader(null) rejects it.
        RawTableCell headerCell = firstRow.get(0);
        Object value = headerCell == null ? null : headerCell.value();
        return XlsHelper.isKnownTableHeader(value == null ? null : value.toString());
    }
}
