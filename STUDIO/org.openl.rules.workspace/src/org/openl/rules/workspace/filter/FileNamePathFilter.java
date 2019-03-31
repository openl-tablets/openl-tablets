package org.openl.rules.workspace.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Filters path names for certain file names.
 *
 * @author Aliaksandr Antonik
 * @author Andrey Naumenko
 */
public class FileNamePathFilter implements PathFilter {
    private final Collection<Pattern> patterns;

    public FileNamePathFilter(Collection<String> filenames) {
        patterns = new ArrayList<>();
        for (String filename : filenames) {
            patterns.add(Pattern.compile("(.*/)?" + filename));
        }
    }

    /**
     * The filter method. Checks a filename in form of: <i>root_folder/sub_folder/.../[file_name]</i>.
     *
     * @param filename file or directory name
     *
     * @return if filter accepts given filename
     */
    @Override
    public boolean accept(String filename) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(filename).matches()) {
                return false;
            }
        }
        return true;
    }
}
