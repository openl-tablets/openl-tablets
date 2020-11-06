package org.openl.rules.project.resolving;

import org.openl.rules.table.properties.ITableProperties;

public interface PropertiesFileNameProcessor {

    ITableProperties process(String modulePath) throws NoMatchFileNameException;

}
