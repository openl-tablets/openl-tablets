package org.openl.rules.project.resolving;

import org.openl.rules.project.model.Module;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.util.FileUtils;

/**
 * Created by dl on 10/20/14.
 *
 * @deprecated CW support for {@code state} property is moved to the default property processor. Delete declaration of
 *             this class from rules.xml
 */
@Deprecated
public class CWPropertyFileNameProcessor extends DefaultPropertiesFileNameProcessor {
    @Override
    public ITableProperties process(Module module, String fileNamePattern) throws NoMatchFileNameException,
                                                                           InvalidFileNamePatternException {
        String path = module.getRulesRootPath().getPath();
        String fileName = FileUtils.getBaseName(path);
        return new DefaultPropertiesFileNameProcessor(fileNamePattern).process(fileName);
    }
}
