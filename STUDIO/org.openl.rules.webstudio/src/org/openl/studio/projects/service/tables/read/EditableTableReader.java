package org.openl.studio.projects.service.tables.read;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Supplier;

import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.formatters.XlsDataFormatterFactory;
import org.openl.studio.projects.model.tables.TableView;

public abstract class EditableTableReader<T extends TableView, R extends TableView.Builder<?>> extends TableReader<T, R> {

    public EditableTableReader(Supplier<R> builderCreator) {
        super(builderCreator);
    }

    public abstract boolean supports(IOpenLTable table);

    protected Object getCellValue(ICell cell, MetaInfoReader metaInfoReader) {
        var value = cell.getObjectValue();
        if (value instanceof String) {
            var metaInfo = metaInfoReader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
            var formatter = XlsDataFormatterFactory.getFormatter(cell, metaInfo, false);
            if (formatter != null) {
                var parsedValue = formatter.parse((String) value);
                if (parsedValue != null) {
                    value = parsedValue;
                }
            }
        }
        if (value instanceof Date) {
            // If the time is not set, then return LocalDate, otherwise LocalDateTime
            var zonedDateTime = ((Date) value).toInstant().atZone(ZoneId.systemDefault());
            if (zonedDateTime.toLocalTime().isAfter(LocalTime.MIN)) {
                value = zonedDateTime.toLocalDateTime();
            } else {
                value = zonedDateTime.toLocalDate();
            }
        }
        return value;
    }
}
