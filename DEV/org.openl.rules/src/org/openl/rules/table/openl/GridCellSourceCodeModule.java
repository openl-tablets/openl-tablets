/*
 * Created on Nov 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table.openl;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.StringUtils;
import org.openl.util.fast.FastStringReader;

/**
 * @author snshor
 *
 */
public class GridCellSourceCodeModule implements IOpenSourceCodeModule {

    private IGridTable table;
    private String code;

    private int row;
    private int column;

    private String uri;

    private Map<String, Object> params;

    public GridCellSourceCodeModule(IGridTable table) {
        this(table, 0, 0, null);
    }

    public IGridTable getTable() {
        return table;
    }

    public GridCellSourceCodeModule(IGridTable table, IBindingContext bindingContext) {
        this(table, 0, 0, bindingContext);
    }

    public GridCellSourceCodeModule(IGridTable table, int column, int row, IBindingContext bindingContext
    // , TableSyntaxNode parent
    ) {
        this.table = table;
        this.column = column;
        this.row = row;
        update(bindingContext);
    }

    public void update(IBindingContext bindingContext) {
        if (bindingContext != null && bindingContext.isExecutionMode()) {
            // init code with value, as table will be turned to null
            //
            initCode();
            // init uri with value, as table will be turned to null
            //
            initUri();
            this.table = null;
        }
    }

    public ICell getCell() {
        return table.getCell(column, row);
    }

    @Override
    public InputStream getByteStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getCharacterStream() {

        return new FastStringReader(getCode());
    }

    @Override
    public String getCode() {
        if (code == null) {
            initCode();
        }
        return code;
    }

    private void initCode() {
        code = table.getCell(column, row).getStringValue();

        if (code == null) {
            code = "";
        }
    }

    public String getDisplayName() {
        return "Cell";
    }

    @Override
    public int getStartPosition() {
        return 0;
    }

    @Override
    public String getUri() {
        if (uri == null) {
            initUri();
        }
        return uri;
    }

    private void initUri() {
        uri = table.getUri(column, row);
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public String toString() {
        if (StringUtils.isNotBlank(code)) {
            return code;
        }
        return super.toString();
    }

}
