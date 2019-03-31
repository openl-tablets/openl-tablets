package org.openl.rules.webstudio.web.repository.project;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nsamatov.
 */
public abstract class TemplatesResolver {
    protected List<String> categories;
    protected final Map<String, List<String>> templateNamesCache = new HashMap<>();

    public List<String> getCategories() {
        if (categories == null) {
            List<String> categoryList = resolveCategories();
            categories = Collections.unmodifiableList(categoryList);
        }
        return categories;
    }

    public List<String> getTemplates(String category) {
        List<String> names = templateNamesCache.get(category);
        if (names != null) {
            return names;
        }

        List<String> templateNames = resolveTemplates(category);
        names = Collections.unmodifiableList(templateNames);

        templateNamesCache.put(category, names);
        return names;
    }

    public abstract ProjectFile[] getProjectFiles(String category, String templateName);

    protected abstract List<String> resolveCategories();

    protected abstract List<String> resolveTemplates(String category);
}
