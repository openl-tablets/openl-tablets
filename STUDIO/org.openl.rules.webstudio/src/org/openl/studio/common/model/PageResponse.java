package org.openl.studio.common.model;

import java.util.Collection;

import io.swagger.v3.oas.annotations.Parameter;

public class PageResponse<T> {

    @Parameter(description = "Current page content")
    private final Collection<T> content;

    @Parameter(description = "Current page number", required = true)
    private final int pageNumber;

    @Parameter(description = "Page size", required = true)
    private final int pageSize;

    @Parameter(description = "Number of items on the current page", required = true)
    private final int numberOfElements;

    public PageResponse(Collection<T> content, int pageNumber, int pageSize) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.numberOfElements = content == null ? 0 : content.size();
    }

    public Collection<T> getContent() {
        return content;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }
}
