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
import org.openl.source.IDependencyManager;
import org.openl.source.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public class GridTableSourceCodeModule implements IOpenSourceCodeModule {

    IGridTable table;
    
    private Map<String, Object> params;
    
    private IDependencyManager depManager;

    public GridTableSourceCodeModule(IGridTable table) {
        this.table = table;
    }

    public InputStream getByteStream() {
        throw new UnsupportedOperationException();
    }

    /**
     *
     */

    public Reader getCharacterStream() {
        throw new UnsupportedOperationException();

    }

    public String getCode() {
        throw new UnsupportedOperationException();
    }

    public int getStartPosition() {
        return 0;
    }

    /**
     *
     */

    public int getTabSize() {
        return 2;
    }

    public String getUri(int textpos) {
        return table.getUri();
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public IDependencyManager getDepManager() {
        return depManager;
    }

    public void setDepManager(IDependencyManager depManager) {
        this.depManager = depManager;
    }
    
    
    
}
