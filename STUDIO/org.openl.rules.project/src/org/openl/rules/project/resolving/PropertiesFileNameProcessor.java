package org.openl.rules.project.resolving;

import org.openl.rules.project.model.Module;
import org.openl.rules.table.properties.ITableProperties;

public interface PropertiesFileNameProcessor {
    ITableProperties process(Module module, String fileNamePattern) throws NoMatchFileNameException,
                                                                    InvalidFileNamePatternException;

}
