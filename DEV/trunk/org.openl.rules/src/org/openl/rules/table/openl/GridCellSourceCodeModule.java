/*
 * Created on Nov 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table.openl;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import org.openl.rules.indexer.IDocumentType;
import org.openl.rules.indexer.IIndexElement;
import org.openl.rules.table.IGridTable;
import org.openl.source.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public class GridCellSourceCodeModule implements IOpenSourceCodeModule, IIndexElement {

    IGridTable table;
    String code;

    // TableSyntaxNode parent;

    int row = 0;
    int column = 0;

    String uri;
    
    private Map<String, Object> params;

    public GridCellSourceCodeModule(IGridTable table) {
        this.table = table;
    }

    public GridCellSourceCodeModule(IGridTable table, int column, int row
    // , TableSyntaxNode parent
    ) {
        this.table = table;
        this.column = column;
        this.row = row;
        // this.parent = parent;
    }

    public InputStream getByteStream() {
        throw new UnsupportedOperationException();
    }

    public String getCategory() {

        return IDocumentType.WORKSHEET_CELL.getCategory();
    }

    public Reader getCharacterStream() {

        return new StringReader(getCode());
    }

    public String getCode() {
        if (code == null) {
            code = table.getCell(column, row).getStringValue();

            if (code == null) {
                code = "";
            }
        }
        return code;
    }

    public String getDisplayName() {
        return "Cell";
    }

    public String getIndexedText() {
        return table.getCell(column, row).getStringValue();
    }

    public int getStartPosition() {
        return 0;
    }

    public int getTabSize() {
        return 2;
    }

    public String getType() {
        return IDocumentType.WORKSHEET_CELL.getCategory();
    }

    public String getUri() {
        if (uri == null) {
            uri = getUri(0);
        }
        return uri;
    }

    public String getUri(int textpos) {
        return table.getUri(column, row);
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
    
}
