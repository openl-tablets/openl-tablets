/*
 * Created on Nov 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table.openl;

import java.io.InputStream;
import java.io.Reader;

import org.openl.rules.table.IGridTable;
import org.openl.source.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public class GridTableSourceCodeModule implements IOpenSourceCodeModule {

    IGridTable table;

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

}
