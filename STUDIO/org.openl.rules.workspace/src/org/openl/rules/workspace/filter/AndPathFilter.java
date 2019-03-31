package org.openl.rules.workspace.filter;

import java.util.List;

/**
 * A {@link org.openl.rules.workspace.filter.PathFilter} providing conditional AND logic across a list of file filters.
 * This filter returns <code>true</code> if all filters in the list return <code>true</code>. Otherwise, it returns
 * <code>false</code>. Checking of the file filter list stops when the first filter returns <code>false</code>.
 *
 * @author Aliaksandr Antonik
 * @author Andrey Naumenko
 */
public class AndPathFilter implements PathFilter {
    private final List<PathFilter> filters;

    public AndPathFilter(List<PathFilter> filters) {
        this.filters = filters;
    }

    /**
     * The filter method. Checks a filename in form of: <i>root_folder/sub_folder/.../[file_name]</i>. Returns
     * <code>true</code> only if all underlying filters return <code>true</code>.
     *
     * @param filename file or directory name
     *
     * @return if filter accepts given filename
     */
    @Override
    public boolean accept(String filename) {
        for (PathFilter filter : filters) {
            if (!filter.accept(filename)) {
                return false;
            }
        }
        return true;
    }
}
