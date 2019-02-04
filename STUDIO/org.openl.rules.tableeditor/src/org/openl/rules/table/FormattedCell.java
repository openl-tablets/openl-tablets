package org.openl.rules.table;

import java.util.Date;

import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ui.CellFont;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.xls.formatters.XlsDataFormatterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author snshor
 */
public class FormattedCell implements ICell {

    private final Logger log = LoggerFactory.getLogger(FormattedCell.class);

    private ICell delegate;

    public ICell getTopLeftCellFromRegion() {
		return delegate.getTopLeftCellFromRegion();
	}

	private ICellFont font;

    private ICellStyle style;

    private IGridFilter filter;

    private Object objectValue;
    private String formattedValue;
    private final CellMetaInfo metaInfo;

    public FormattedCell(ICell delegate, CellMetaInfo cellMetaInfo) {
        this.delegate = delegate;
        this.objectValue = this.delegate.getObjectValue();
        this.formattedValue = XlsDataFormatterFactory.getFormattedValue(delegate, cellMetaInfo);

        this.font = new CellFont(delegate.getFont());
        this.style = new CellStyle(delegate.getStyle());
        this.metaInfo = cellMetaInfo;
    }

    public ICellStyle getStyle() {
        return style;
    }

    public ICellFont getFont() {
        return font;
    }

    public Object getObjectValue() {
        return objectValue;
    }

    public void setObjectValue(Object objectValue) {
        this.objectValue = objectValue;
    }

    public IGridFilter getFilter() {
        return filter;
    }

    public void setFilter(IGridFilter filter) {
        if (this.filter != null) {
            log.warn("More than one filter set on cell");
        }
        this.filter = filter;
    }

    public String getFormattedValue() {
        return formattedValue;
    }

    public void setFormattedValue(String formattedValue) {
        this.formattedValue = formattedValue;
    }

    public int getAbsoluteColumn() {
        return delegate.getAbsoluteColumn();
    }

    public int getAbsoluteRow() {
        return delegate.getAbsoluteRow();
    }

    public IGridRegion getAbsoluteRegion() {
        return delegate.getAbsoluteRegion();
    }

    public int getRow() {
        return delegate.getRow();
    }

    public int getColumn() {
        return delegate.getColumn();
    }

    public IGridRegion getRegion() {
        return delegate.getRegion();
    }

    public int getHeight() {
        return delegate.getHeight();
    }

    public String getStringValue() {
        return delegate.getStringValue();
    }

    public int getWidth() {
        return delegate.getWidth();
    }

    public String getFormula() {
        return delegate.getFormula();
    }

    public int getType() {
        return delegate.getType();
    }

    public String getUri() {
        return delegate.getUri();
    }

    public boolean getNativeBoolean() {
        return delegate.getNativeBoolean();
    }

    public double getNativeNumber() {
        return delegate.getNativeNumber();
    }

    public int getNativeType() {
        return delegate.getNativeType();
    }

    public boolean hasNativeType() {
        return delegate.hasNativeType();
    }

    public Date getNativeDate() {
        return delegate.getNativeDate();
    }

    public CellMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public ICellComment getComment() {
        return delegate.getComment();
    }

}
