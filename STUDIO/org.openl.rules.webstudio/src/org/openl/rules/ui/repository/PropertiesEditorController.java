package org.openl.rules.ui.repository;

import org.openl.rules.webstudio.util.FacesUtils;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Properties editor controller.
 *
 * @author Andrey Naumenko
 */
public class PropertiesEditorController {
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        Object dataBean = FacesUtils.getFacesVariable(
                "#{repositoryTree.selected.dataBean}");

        return properties;
    }
}
