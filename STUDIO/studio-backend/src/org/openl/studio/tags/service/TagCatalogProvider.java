package org.openl.studio.tags.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * The tag catalog of the current request.
 *
 * <p>Listing projects checks the tags of every project on the page, and of every project in the facet
 * scope on top of that. The configured types and values are the same for all of them, so they are read
 * once per request instead of once per project or per tag.
 */
@Component
@RequestScope
@RequiredArgsConstructor
public class TagCatalogProvider {

    private final TagTypeService tagTypeService;
    private final TagService tagService;

    private TagCatalog catalog;

    /** The configured tag types and values, read once for this request. */
    public TagCatalog get() {
        if (catalog == null) {
            catalog = TagCatalog.of(tagTypeService.getAllTagTypes(), tagService.getAll());
        }
        return catalog;
    }

    /**
     * Drops the snapshot, so the next read sees the configuration again.
     *
     * <p>Assigning a new value of an extensible tag type configures it, and the rest of the request must
     * see it as configured.
     */
    public void invalidate() {
        catalog = null;
    }
}
