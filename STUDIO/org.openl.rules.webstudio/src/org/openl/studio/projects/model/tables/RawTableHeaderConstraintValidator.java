package org.openl.studio.projects.model.tables;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.util.StringUtils;

/**
 * Checks that a raw table has a recognized OpenL header, or an explicit free-form header for the {@code Other} kind.
 *
 * @author Vladyslav Pikus
 */
public class RawTableHeaderConstraintValidator implements ConstraintValidator<RawTableHeaderConstraint, RawTableView> {

    private static final String FREE_FORM_KIND = OpenLTableUtils.getTableTypeItems()
            .get(XlsNodeTypes.XLS_OTHER.toString());

    @Override
    public boolean isValid(RawTableView view, ConstraintValidatorContext context) {
        if (view == null || view.source == null) {
            return true; // a missing source is already reported by @NotNull
        }
        if (view.source.isEmpty()) {
            return false; // a table with no cells has no header (an empty matrix is only a valid read result)
        }
        var firstRow = view.source.get(0);
        if (firstRow == null || firstRow.isEmpty()) {
            return true; // an empty first row is a structural issue reported by other constraints
        }
        // A null cell or a null/blank value at the top-left is no header at all; isKnownTableHeader(null) rejects it.
        var headerCell = firstRow.get(0);
        Object value = headerCell == null ? null : headerCell.value();
        var header = value == null ? null : value.toString();
        return FREE_FORM_KIND.equals(view.kind)
                ? StringUtils.isNotBlank(header)
                : XlsHelper.isKnownTableHeader(header);
    }
}
