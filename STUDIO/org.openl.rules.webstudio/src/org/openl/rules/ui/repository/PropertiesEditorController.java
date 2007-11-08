package org.openl.rules.ui.repository;

import org.openl.rules.webstudio.util.FacesUtils;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.impl.PropertyImpl;

import java.util.ArrayList;
import java.util.List;


/**
 * Properties editor controller.
 *
 * @author Andrey Naumenko
 */
public class PropertiesEditorController {
    public List<Property> getProperties() {
        List<Property> properties = new ArrayList<Property>();

        Object dataBean = FacesUtils.getFacesVariable(
                "#{repositoryTree.selected.dataBean}");

        if (dataBean instanceof ProjectResource) {}
        else if (dataBean instanceof Project) {
            properties.add(new PropertyImpl("Effective Date", "09/01/2007"));
            properties.add(new PropertyImpl("Expiration Date", "01/01/2008"));
            properties.add(new PropertyImpl("LOB", ""));
            properties.add(new PropertyImpl("Region", ""));
        } else if (dataBean instanceof ProjectFolder) {}

        return properties;
    }
}
