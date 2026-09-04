package org.openl.rules.table.actions;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.helpers.INumberRange;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;

/**
 * @author snshor
 */
public class UndoableSetValueAction extends AUndoableCellAction {

    private final Object newValue;

    public UndoableSetValueAction(int col, int row, Object value, MetaInfoWriter metaInfoWriter) {
        super(col, row, metaInfoWriter);
        this.newValue = value;
    }

    @Override
    public void doAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();

        var cell = grid.getCell(getCol(), getRow());
        setPrevValue(cell.getObjectValue());
        setPrevFormula(cell.getFormula());
        setPrevMetaInfo(metaInfoWriter.getMetaInfo(getRow(), getCol()));

        var convertedValue = convertToCellType(newValue);
        grid.setCellValue(getCol(), getRow(), convertedValue);
        var newMetaInfo = getNewMetaInfo(convertedValue);
        if (newMetaInfo != null) {
            metaInfoWriter.setMetaInfo(getRow(), getCol(), newMetaInfo);
        }
    }

    private Object convertToCellType(Object value) {
        var metaInfo = metaInfoWriter.getMetaInfo(getRow(), getCol());
        if (metaInfo != null && metaInfo.getDataType() != null) {
            var targetType = metaInfo.getDataType().getInstanceClass();
            if (metaInfo.isMultiValue() && value instanceof Object[] values) {
                return convertMultiValue(values, targetType);
            }
            return convertToType(value, targetType);
        }
        return value;
    }

    private static Object convertMultiValue(Object[] values, Class<?> targetType) {
        var convertedValues = new Object[values.length];
        var canUseTargetArrayType = !targetType.isPrimitive();
        for (var i = 0; i < values.length; i++) {
            convertedValues[i] = convertToType(values[i], targetType);
            if (convertedValues[i] != null && !targetType.isInstance(convertedValues[i])) {
                canUseTargetArrayType = false;
            }
        }
        if (!canUseTargetArrayType) {
            return convertedValues;
        }
        var typedValues = (Object[]) Array.newInstance(targetType, convertedValues.length);
        System.arraycopy(convertedValues, 0, typedValues, 0, convertedValues.length);
        return typedValues;
    }

    private static Object convertToType(Object value, Class<?> targetType) {
        if (!(value instanceof String stringValue) || targetType == String.class) {
            return value;
        }
        try {
            return String2DataConvertorFactory.getConvertor(targetType).parse(stringValue, null);
        } catch (Exception ignored) {
            return value;
        }
    }

    @Override
    public void undoAction(IGridTable table) {
        var grid = (IWritableGrid) table.getGrid();
        if (StringUtils.isNotBlank(getPrevFormula())) {
            grid.setCellFormula(getCol(), getRow(), getPrevFormula());
        } else {
            grid.setCellValue(getCol(), getRow(), getPrevValue());
        }
        metaInfoWriter.setMetaInfo(getRow(), getCol(), getPrevMetaInfo());
    }

    private CellMetaInfo getNewMetaInfo(Object value) {
        if (value == null) {
            return null;
        }
        var prevMetaInfo = getPrevMetaInfo();
        if (prevMetaInfo != null
                && prevMetaInfo.isMultiValue()
                && value instanceof Object[] values
                && matchesMultiValueType(values, prevMetaInfo)) {
            return removeNodeUsage(prevMetaInfo);
        }
        IOpenClass newType = JavaOpenClass.getOpenClass(value.getClass());
        if (prevMetaInfo != null && prevMetaInfo.getDataType() != null && prevMetaInfo.getDataType().equals(newType)) {
            return removeNodeUsage(prevMetaInfo);
        }

        IOpenClass dataType = prevMetaInfo == null ? null : prevMetaInfo.getDataType();
        if (dataType != null) {
            IDomain<?> domain = dataType.getDomain();
            var keepOldMetaInfo = domain instanceof EnumDomain<?> || ClassUtils
                    .isAssignable(dataType.getInstanceClass(), INumberRange.class);
            if (keepOldMetaInfo) {
                // Don't change meta info
                return removeNodeUsage(prevMetaInfo);
            }
        }

        var multiValue = false;
        if (newType.getAggregateInfo().isAggregate(newType)) {
            newType = newType.getAggregateInfo().getComponentType(newType);
            multiValue = true;
        }

        return new CellMetaInfo(newType, multiValue);
    }

    private static boolean matchesMultiValueType(Object[] values, CellMetaInfo metaInfo) {
        var dataType = metaInfo.getDataType();
        if (dataType == null) {
            return false;
        }
        var elementType = dataType.getInstanceClass();
        return Arrays.stream(values)
                .allMatch(value -> value == null ? !elementType.isPrimitive()
                        : ClassUtils.isAssignable(value.getClass(), elementType));
    }

    /**
     * Remove NodeUsage for a new value because it can contain another string so NodeUsage will be incorrect.
     *
     * @param metaInfo old meta info
     * @return new meta info
     */
    private CellMetaInfo removeNodeUsage(CellMetaInfo metaInfo) {
        return new CellMetaInfo(metaInfo.getDataType(), metaInfo.isMultiValue());
    }

}
