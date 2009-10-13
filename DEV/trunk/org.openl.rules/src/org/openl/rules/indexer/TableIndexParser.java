package org.openl.rules.indexer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;


/**
 * Parser for tables. Parses the table to cells it contain. see {@link GridCellSourceCodeModule}
 * 
 */
public class TableIndexParser implements IIndexParser {
    
    private static final Log LOG = LogFactory.getLog(TableIndexParser.class);

    public String getCategory() {
        return IDocumentType.WORKSHEET_TABLE.getCategory();
    }

    public String getType() {
        return "All";
    }
    
    /**
     * Parses the table to cells it contain. see {@link GridCellSourceCodeModule}
     * 
     * @param root Table for parsing.
     * @return Array of grid cells of this table.
     */
    public IIndexElement[] parse(IIndexElement root) {
        TableSyntaxNode tableSrc = (TableSyntaxNode) root;

        IGridTable table = tableSrc.getTable().getGridTable();

        int w = table.getLogicalWidth();
        int h = table.getLogicalHeight();

        List<GridCellSourceCodeModule> gridCells = new ArrayList<GridCellSourceCodeModule>();

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                String cellValue = null;
                try {
                    cellValue = table.getCell(j, i).getStringValue();                    
                } catch (RuntimeException e) {
                    LOG.warn("There is an error in cell in table:["+tableSrc.getDisplayName()+"]", e);
                }
                if (cellValue != null) {
                    gridCells.add(new GridCellSourceCodeModule(table, j, i));
                }
                
            }
        }

        return (GridCellSourceCodeModule[]) gridCells.toArray(new GridCellSourceCodeModule[gridCells.size()]);
    }

}
