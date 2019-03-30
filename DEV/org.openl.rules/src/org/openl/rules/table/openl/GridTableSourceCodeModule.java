/*
 * Created on Nov 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table.openl;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.openl.rules.table.IGridTable;
import org.openl.source.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public class GridTableSourceCodeModule implements IOpenSourceCodeModule {

    IGridTable table;

    private Map<String, Object> params;

    public GridTableSourceCodeModule(IGridTable table) {
        this.table = table;
    }

    @Override
    public InputStream getByteStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getCharacterStream() {
        throw new UnsupportedOperationException();

    }

    @Override
    public String getCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStartPosition() {
        return 0;
    }

    @Override
    public String getUri() {
        return table.getUri();
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

}
