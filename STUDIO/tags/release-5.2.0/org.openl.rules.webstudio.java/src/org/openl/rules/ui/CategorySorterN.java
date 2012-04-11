/**
 * Created Apr 3, 2007
 */
package org.openl.rules.ui;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.StringTool;

/**
 * @author snshor
 *
 */
public class CategorySorterN extends CategorySorter {

    int categoryLevel;
    String separators;

    public CategorySorterN(int categoryLevel, String separators) {
        this.categoryLevel = categoryLevel;
        this.separators = separators;
    }

    @Override
    String getCategory(TableSyntaxNode tsn) {
        String category = super.getCategory(tsn);

        String[] categories = StringTool.tokenize(category, separators);

        return categoryLevel < categories.length ? categories[categoryLevel] : categories[categories.length - 1];
    }

    @Override
    public String getName() {
        return "category." + categoryLevel;
    }

    @Override
    public String getType(Object sorterObject) {
        return "category." + categoryLevel;
    }

}
