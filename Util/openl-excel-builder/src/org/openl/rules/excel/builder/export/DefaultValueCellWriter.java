package org.openl.rules.excel.builder.export;

import static org.openl.rules.excel.builder.export.AbstractOpenlTableExporter.DEFAULT_STRING_VALUE;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.Model;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultValueCellWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultValueCellWriter.class);

    protected DefaultValueCellWriter() {
    }

    public static void writeDefaultValueToCell(Model model,
            FieldModel field,
            Cell valueCell,
            CellStyle dateStyle,
            CellStyle dateTimeStyle) {
        if (field.getDefaultValue() == null) {
            valueCell.setCellValue("");
        } else {
            try {
                setDefaultValue(field, valueCell, dateStyle, dateTimeStyle);
            } catch (ParseException e) {
                LOGGER
                    .error("Error is occurred on writing field: {}, model: {} .", field.getName(), model.getName(), e);
            }
        }
    }

    private static void setDefaultValue(FieldModel model,
            Cell valueCell,
            CellStyle dateStyle,
            CellStyle dateTimeStyle) throws ParseException {
        Object defaultValue = model.getDefaultValue();
        String valueAsString = defaultValue.toString();
        switch (model.getType()) {
            case "Integer":
            case "BigInteger":
                Number casted = NumberFormat.getInstance().parse(valueAsString);
                if (casted.longValue() <= Integer.MAX_VALUE) {
                    valueCell.setCellValue(Integer.parseInt(valueAsString));
                } else {
                    valueCell.setCellValue(Long.parseLong(valueAsString));
                }
                break;
            case "Long":
                valueCell.setCellValue(Long.parseLong(valueAsString));
                break;
            case "Double":
                valueCell.setCellValue(Double.parseDouble(valueAsString));
                break;
            case "Float":
                valueCell.setCellValue(new BigDecimal(valueAsString).doubleValue());
                break;
            case "BigDecimal":
                valueCell.setCellValue(valueAsString);
                break;
            case "String":
                if (StringUtils.isBlank(valueAsString)) {
                    valueCell.setCellValue(DEFAULT_STRING_VALUE);
                } else {
                    valueCell.setCellValue(valueAsString);
                }
                break;
            case "Boolean":
                valueCell.setCellValue(Boolean.parseBoolean(valueAsString));
                break;
            case "Date":
                if (defaultValue instanceof Date) {
                    Date dateValue = (Date) defaultValue;
                    valueCell.setCellValue(dateValue);
                    valueCell.setCellStyle(dateStyle);
                } else {
                    OffsetDateTime dateValue = (OffsetDateTime) defaultValue;
                    valueCell.setCellValue(new Date((dateValue).toInstant().toEpochMilli()));
                    valueCell.setCellStyle(dateTimeStyle);
                }
                break;
            default:
                valueCell.setCellValue("");
                break;
        }
    }
}
