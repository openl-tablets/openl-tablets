package org.openl.rules.ui.repository;

import org.openl.jsf.FacesUtils;

import org.openl.rules.commons.props.Property;
import org.openl.rules.commons.props.impl.PropertyImpl;
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
            properties.add(new PropertyImpl("Property1", "1"));
            properties.add(new PropertyImpl("Property2", "2"));
        }

        return properties;
    }
}
