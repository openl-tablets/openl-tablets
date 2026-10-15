package org.openl.studio.projects.service.tables.theme;

import java.time.temporal.Temporal;

import org.apache.commons.lang3.ClassUtils;
import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.element.SpreadsheetExpressionMarker;
import org.openl.rules.helpers.DateRange;
import org.openl.rules.helpers.INumberRange;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DecisionTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.studio.projects.model.tables.RawTableValueType;
import org.openl.types.IOpenClass;

/**
 * The type of the value of a cell, as the compiler reads it, never as its text reads.
 *
 * <p>A value written as an expression, starting with {@code =}, is code. Any other value takes the type the
 * compiler bound it to: a number or a range of numbers, a date or a range of dates, a boolean, and anything else a
 * string. An array takes the type of its elements. A value the compiler bound to nothing takes the type Excel holds it
 * in, a number, a boolean or a text.
 *
 * @author Yury Molchan
 */
// Rules take values of java.util.Date, named in full: the type of such a value is told by that class, not chosen.
@SuppressWarnings("java:S2143")
final class ValueTypes {

    private final @Nullable MetaInfoReader reader;

    private ValueTypes(@Nullable MetaInfoReader reader) {
        this.reader = reader;
    }

    /**
     * The types of the values of a compiled table.
     *
     * <p>A decision table keeps what it knows of its cells for the window a read asks for, so its types are read
     * through a reader of their own, which no read shares.
     *
     * @param node   the table, compiled
     * @param region where the table stands on its sheet
     * @return the types of its values
     */
    static ValueTypes of(TableSyntaxNode node, IGridRegion region) {
        var reader = node.getMetaInfoReader();
        if (reader instanceof DecisionTableMetaInfoReader decision) {
            reader = new DecisionTableMetaInfoReader(decision.getBoundNode());
            reader.prepare(region);
        }
        return new ValueTypes(reader);
    }

    /**
     * The type of the value of a cell.
     *
     * @param cell   the cell, holding a merged region or merged with none
     * @param typing how the place of the cell types its value
     * @return the type, or {@code null} for a cell holding no value, or a place holding no value to type
     */
    @Nullable
    RawTableValueType of(ICell cell, Place.Typing typing) {
        var text = cell.getStringValue();
        if (typing == Place.Typing.NONE || text == null || text.isBlank()) {
            return null;
        }
        if (typing == Place.Typing.CODE || SpreadsheetExpressionMarker.isFormula(text.trim())) {
            return RawTableValueType.CODE;
        }
        var meta = reader == null ? null : reader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        var type = meta == null ? null : meta.getDataType();
        return type == null ? nativeTypeOf(cell) : typeOf(type);
    }

    /** The type of a class the compiler bound a value to. */
    static RawTableValueType typeOf(IOpenClass type) {
        var clazz = type.getInstanceClass();
        while (clazz != null && clazz.isArray()) {
            clazz = clazz.getComponentType();
        }
        if (clazz == null) {
            return RawTableValueType.STRING;
        }
        var boxed = ClassUtils.primitiveToWrapper(clazz);
        if (Number.class.isAssignableFrom(boxed) || INumberRange.class.isAssignableFrom(boxed)) {
            return RawTableValueType.NUMBER;
        }
        if (java.util.Date.class.isAssignableFrom(boxed) || Temporal.class.isAssignableFrom(boxed)
                || DateRange.class.isAssignableFrom(boxed)) {
            return RawTableValueType.DATE;
        }
        return Boolean.class.equals(boxed) ? RawTableValueType.BOOLEAN : RawTableValueType.STRING;
    }

    /** The type Excel holds a value in that the compiler bound to nothing. */
    private static RawTableValueType nativeTypeOf(ICell cell) {
        return switch (cell.getNativeType()) {
            case IGrid.CELL_TYPE_NUMERIC -> cell.getObjectValue() instanceof java.util.Date
                    ? RawTableValueType.DATE
                    : RawTableValueType.NUMBER;
            case IGrid.CELL_TYPE_BOOLEAN -> RawTableValueType.BOOLEAN;
            default -> RawTableValueType.STRING;
        };
    }
}
