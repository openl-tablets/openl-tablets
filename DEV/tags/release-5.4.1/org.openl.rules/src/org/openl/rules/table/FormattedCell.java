/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.table.ui.CellFont;
import org.openl.rules.table.ui.CellStyle;
import org.openl.rules.table.ui.filters.IGridFilter;
import org.openl.rules.table.xls.IncorrectFormulaException;

/**
 * @author snshor
 *
 */
public class FormattedCell implements ICell {
    
    private final static Log LOG = LogFactory.getLog(FormattedCell.class); 
    
    private ICell delegate;

    private CellFont font;

    private CellStyle style;

    private IGridFilter filter;
    
    private Object objectValue;
    private String formattedValue;

    public FormattedCell(ICell delegate) {
        this.delegate = delegate;
        try {
            this.objectValue = this.delegate.getObjectValue();
        } catch (IncorrectFormulaException e) { 
            //logged in XlsCell.getObjectValue() method.
            this.objectValue = ERROR_VALUE;
        }
        this.formattedValue = this.delegate.getStringValue();
                
        this.font = new CellFont(delegate.getFont());
        this.style = new CellStyle(delegate.getStyle());
    }

    public CellStyle getStyle() {
        return style;
    }

    public CellFont getFont() {
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
            LOG.warn("More than one filter set on cell");
        }
        this.filter = filter;
    }

    public String getFormattedValue() {
        return formattedValue;
    }

    public void setFormattedValue(String formattedValue) {
        this.formattedValue = formattedValue;
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
}
