package org.openl.rules.webstudio.services.upload;

import java.util.Arrays;
import java.util.Collection;

public class CompoundUploadFilter implements UploadFilter{
    private final Collection<UploadFilter> filters;

    public CompoundUploadFilter(UploadFilter... filter) {
        filters = Arrays.asList(filter);
    }

    /**
     * The filter method. Checks a filename in form of: <i>root_folder/sub_folder/.../[file_name]</i>. Returns
     * <code>true</code> only if all underlying filters return <code>true</code>. 
     *
     * @param filename file or directory name
     * @return if filter accepts given filename
     */
    public boolean accept(String filename) {
        for (UploadFilter filter : filters)
            if (!filter.accept(filename))
                return false;
        return true;
    }
}
