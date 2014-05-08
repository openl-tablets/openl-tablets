package org.openl.rules.webstudio.web.repository.project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nsamatov.
 */
public abstract class TemplatesResolver {
    protected String[] categories;
    protected final Map<String, String[]> templateNamesCache = new HashMap<String, String[]>();

    public String[] getCategories() {
        if (categories == null) {
            List<String> categoryList = resolveCategories();
            categories = categoryList.toArray(new String[categoryList.size()]);
        }
        return categories;
    }

    public String[] getTemplates(String category) {
        String[] names = templateNamesCache.get(category);
        if (names != null) {
            return names;
        }

        List<String> templateNames = resolveTemplates(category);
        names = templateNames.toArray(new String[templateNames.size()]);

        templateNamesCache.put(category, names);
        return names;
    }

    public abstract ProjectFile[] getProjectFiles(String category, String templateName);

    protected abstract List<String> resolveCategories();

    protected abstract List<String> resolveTemplates(String category);
}
