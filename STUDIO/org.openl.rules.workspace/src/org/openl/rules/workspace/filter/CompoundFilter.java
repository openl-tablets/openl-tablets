package org.openl.rules.workspace.filter;

import java.util.Arrays;
import java.util.List;


public class CompoundFilter implements PathFilter {
    private final List<PathFilter> filters;

    public CompoundFilter(PathFilter... filters) {
        this.filters = Arrays.asList(filters);
    }

    public CompoundFilter(List<PathFilter> filters) {
        this.filters = filters;
    }

    /**
     * The filter method. Checks a filename in form of:
     * <i>root_folder/sub_folder/.../[file_name]</i>. Returns <code>true</code> only if
     * all underlying filters return <code>true</code>.
     *
     * @param filename file or directory name
     *
     * @return if filter accepts given filename
     */
    public boolean accept(String filename) {
        for (PathFilter filter : filters) {
            if (!filter.accept(filename)) {
                return false;
            }
        }
        return true;
    }
}
