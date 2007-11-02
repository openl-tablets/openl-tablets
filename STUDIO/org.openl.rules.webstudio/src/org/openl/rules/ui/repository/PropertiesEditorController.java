package org.openl.rules.ui.repository;

import org.openl.jsf.FacesUtils;

import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.impl.PropertyImpl;
import org.openl.rules.ui.repository.beans.FileBean;
import org.openl.rules.ui.repository.beans.FolderBean;
import org.openl.rules.ui.repository.beans.ProjectBean;

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
                "#{userSession.repositoryTree.selected.dataBean}");

        if (dataBean instanceof FileBean) {}
        else if (dataBean instanceof FolderBean) {}
        else if (dataBean instanceof ProjectBean) {
            properties.add(new PropertyImpl("Effective Date", "09/01/2007"));
            properties.add(new PropertyImpl("Expiration Date", "01/01/2008"));
            properties.add(new PropertyImpl("LOB", ""));
            properties.add(new PropertyImpl("Region", ""));
        }

        return properties;
    }
}
