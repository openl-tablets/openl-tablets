/**
 * Created May 4, 2007
 */
package org.openl.rules.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * @author snshor
 *
 */
public class OpenLAdvancedSearchResult {    

    private ArrayList<TableAndRows> foundTables = new ArrayList<TableAndRows>();

    public OpenLAdvancedSearchResult() {}

    public void add(TableSyntaxNode tsn, ISearchTableRow[] rows) {
        foundTables.add(new TableAndRows(tsn, rows));
    }
 
    public TableAndRows[] getFoundTableAndRows() {
        TableAndRows[] tr = foundTables.toArray(new TableAndRows[0]);

        Arrays.sort(tr, new Comparator<TableAndRows>() {

            public int compare(TableAndRows t1, TableAndRows t2) {
                return t2.rows.length - t1.rows.length;
            }
        });
        return tr;
    }
    
    public static class TableAndRows {
        private TableSyntaxNode tsn;
        private ISearchTableRow[] rows;

        public TableAndRows(TableSyntaxNode tsn, ISearchTableRow[] rows) {
            this.rows = rows;
            this.tsn = tsn;
        }

        public ISearchTableRow[] getRows() {
            return rows;
        }

        public TableSyntaxNode getTsn() {
            return tsn;
        }
    }

}
