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
        if (firstRow == null || firstRow.isEmpty() || firstRow.get(0) == null) {
            return true; // a missing header cell is a structural issue reported elsewhere
        }
        Object value = firstRow.get(0).value();
        return XlsHelper.isKnownTableHeader(value == null ? null : value.toString());
    }
}
