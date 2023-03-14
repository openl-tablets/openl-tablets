package org.openl.rules.repository.api;

import java.io.IOException;
import java.util.List;

/**
 * Searchable repository API
 *
 * @author Vladyslav Pikus
 */
public interface SearchableRepository extends Repository {

    /**
     * Gets paged commit history for project by {@code globalFilter} if provided
     * 
     * @param name Project name
     * @param globalFilter global filer allows regexp.
     * @param page page to display
     * @return page log
     * @throws IOException error
     */
    List<FileData> listHistory(String name, String globalFilter, boolean techRevs, Pageable pageable) throws IOException;

}
