package org.openl.rules.indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
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
                
        Map<String, IGridRegion> processedValuesFromRegions = new HashMap<String, IGridRegion>();
        
        
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                String cellValue = null;
                ICell cell = null;
                try {
                    cell = table.getCell(j, i); 
                    cellValue = cell.getStringValue();
                } catch (RuntimeException e) {
                    LOG.warn("There is an error in cell in table:["+tableSrc.getDisplayName()+"]", e);
                }
                
                if (cellValue != null) {                    
                    if (!isCellBelongingToProcessedRegion(cell, cellValue, processedValuesFromRegions)) {
                        if (isCellFromMergedRegion(cell)) {
                            processedValuesFromRegions.put(cellValue, cell.getRegion());
                        }                        
                        gridCells.add(new GridCellSourceCodeModule(table, j, i));                       
                    }
                }
            }
        }

        return (GridCellSourceCodeModule[]) gridCells.toArray(new GridCellSourceCodeModule[gridCells.size()]);
    }
    
    /**
     * Check if cell belongs to some merged region.
     * 
     * @param cell 
     * @return true if cell belongs to some merged region
     */
    private boolean isCellFromMergedRegion(ICell cell) {
        return cell.getRegion() != null;
    }
    
    /**
     * Checks if current cell belongs to some previously processed merged region.
     * 
     * @param cell current cell.
     * @param cellValue cellValue
     * @param processedValuesFromRegions values of the cells that belong to some merged regions.
     * @return true if current cell belongs to some previously processed merged region.
     */
    private boolean isCellBelongingToProcessedRegion(ICell cell,
            String cellValue,
            Map<String, IGridRegion> processedValuesFromRegions) {
        
        boolean cellBelongsToProssedRegion = false;
        if (isCellFromMergedRegion(cell)) {
            if (processedValuesFromRegions.containsKey(cellValue)) { // if cellValue from region was previosly 
                                                                     // processed we need to check if current cell 
                                                                     // belongs to processed region.
                cellBelongsToProssedRegion = IGridRegion.Tool.contains(processedValuesFromRegions.get(cellValue),
                    cell.getAbsoluteColumn(),
                    cell.getAbsoluteRow());
            }
        }
        return cellBelongsToProssedRegion;
    }
    
//  private void prepareColumns(ILogicalTable tableBody) throws SyntaxNodeException {
//  columns = new ArrayList<TableColumn>();
//  Set<String> addedIds = new HashSet<String>();
//
//  ILogicalTable ids = tableBody.getLogicalRow(0);
//  
//  Map<String, IGridRegion> valuesFromRegions = new HashMap<String, IGridRegion>();
//  // parse ids, row=0
//  for (int c = 0; c < ids.getLogicalWidth(); c++) {
//      ICell columnCell = ids.getLogicalColumn(c).getGridTable().getCell(0, 0);
//      
//      String id = safeId(columnCell.getStringValue());
//      if (id.length() == 0) {
//          // ignore column with NO ID
//          continue;
//      }
//      
//      if (addedIds.contains(id)) {
//          // duplicate ids
//          throw SyntaxNodeExceptionUtils.createError("Duplicate column '" + id + "'!", null, tsn);
//      }
//      
////      boolean cellBelongsToProssedRegion = false;
////      if (addedIds.contains(id)) {
////          if (isCellFromMergedRegion(columnCell)) {
////              if (valuesFromRegions.containsKey(id)) { // if id from region was previosly processed we need 
////                                                       // to check if current cell belongs to processed region. 
////                  cellBelongsToProssedRegion = IGridRegion.Tool.contains(valuesFromRegions.get(id), columnCell.getAbsoluteColumn(), columnCell.getAbsoluteRow());                        
////              }
////          }
////          if (!cellBelongsToProssedRegion) {
////              // duplicate ids
////              throw SyntaxNodeExceptionUtils.createError("Duplicate column '" + id + "'!", null, tsn);
////          } 
////          
////      } else {
////          if (isCellFromMergedRegion(columnCell)) {
////              valuesFromRegions.put(id, columnCell.getRegion());
////          }
////          columns.add(new TableColumn(id, c));
////          addedIds.add(id);
////      }
//  }
//}


}
