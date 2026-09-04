package org.openl.rules.table;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ui.CellFont;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.xls.formatters.XlsDataFormatterFactory;

/**
 * @author snshor
 */
@Slf4j
public class FormattedCell implements ICell {


    private final ICell delegate;

    @Override
    public ICell getTopLeftCellFromRegion() {
        return delegate.getTopLeftCellFromRegion();
    }

    @Getter
    private final ICellFont font;

    @Getter
    private final ICellStyle style;

    @Getter
    private IGridFilter filter;

    @Getter
    @Setter
    private Object objectValue;
    @Getter
    @Setter
    private String formattedValue;
    @Getter
    private final CellMetaInfo metaInfo;

    public FormattedCell(ICell delegate, CellMetaInfo cellMetaInfo) {
        this.delegate = delegate;
        this.objectValue = this.delegate.getObjectValue();
        this.formattedValue = XlsDataFormatterFactory.getFormattedValue(delegate, cellMetaInfo, false);

        this.font = new CellFont(delegate.getFont());
        this.style = new CellStyle(delegate.getStyle());
        this.metaInfo = cellMetaInfo;
    }

    public void setFilter(IGridFilter filter) {
        if (this.filter != null) {
            log.warn("More than one filter set on cell");
        }
        this.filter = filter;
    }

    @Override
    public int getAbsoluteColumn() {
        return delegate.getAbsoluteColumn();
    }

    @Override
    public int getAbsoluteRow() {
        return delegate.getAbsoluteRow();
    }

    @Override
    public IGridRegion getAbsoluteRegion() {
        return delegate.getAbsoluteRegion();
    }

    @Override
    public int getRow() {
        return delegate.getRow();
    }

    @Override
    public int getColumn() {
        return delegate.getColumn();
    }

    @Override
    public IGridRegion getRegion() {
        return delegate.getRegion();
    }

    @Override
    public int getHeight() {
        return delegate.getHeight();
    }

    @Override
    public String getStringValue() {
        return delegate.getStringValue();
    }

    @Override
    public int getWidth() {
        return delegate.getWidth();
    }

    @Override
    public String getFormula() {
        return delegate.getFormula();
    }

    @Override
    public int getType() {
        return delegate.getType();
    }

    @Override
    public String getUri() {
        return delegate.getUri();
    }

    @Override
    public boolean getNativeBoolean() {
        return delegate.getNativeBoolean();
    }

    @Override
    public double getNativeNumber() {
        return delegate.getNativeNumber();
    }

    @Override
    public int getNativeType() {
        return delegate.getNativeType();
    }

    @Override
    public boolean hasNativeType() {
        return delegate.hasNativeType();
    }

    @Override
    public Date getNativeDate() {
        return delegate.getNativeDate();
    }

    @Override
    public ICellComment getComment() {
        return delegate.getComment();
    }

}
